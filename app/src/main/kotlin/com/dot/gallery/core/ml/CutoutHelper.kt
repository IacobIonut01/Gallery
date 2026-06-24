/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.os.PersistableBundle
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.app.ShareCompat
import com.dot.gallery.BuildConfig
import com.dot.gallery.R
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.core.util.ext.saveImage
import com.dot.gallery.feature_node.data.data_source.KeychainHolder
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.domain.util.isEncrypted
import com.dot.gallery.feature_node.presentation.util.createDecryptedTempFile
import com.dot.gallery.feature_node.presentation.util.resizeBitmap
import com.dot.gallery.feature_node.presentation.util.resolveShareableUri
import com.dot.gallery.feature_node.presentation.util.printDebug
import com.dot.gallery.feature_node.presentation.util.printError
import com.dot.gallery.feature_node.presentation.util.printInfo
import ai.onnxruntime.providers.NNAPIFlags
import java.util.EnumSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.util.Collections

object CutoutHelper {

    data class CutoutResult(
        val bitmap: Bitmap,
        val originalBounds: Rect
    )

    data class PromptPoint(
        val x: Float, // Original image coordinates
        val y: Float,
        val isPositive: Boolean
    )

    /**
     * Active session for performing MobileSAM cutout operations
     */
    class CutoutSession(
        private val context: Context,
        private val media: Media,
        private val modelManager: ModelManager
    ) {
        private var env: OrtEnvironment = OrtEnvironment.getEnvironment()
        private var encoderSession: OrtSession? = null
        private var decoderSession: OrtSession? = null

        // Cached encoder outputs
        private var imageEmbeddings: OnnxTensor? = null

        // Cached original bitmap
        private var originalBitmap: Bitmap? = null

        // SAM Scale & Pad parameters
        private var scaleSam: Float = 1.0f
        private var newH: Int = 0
        private var newW: Int = 0

        var widthOrig: Int = 0
            private set
        var heightOrig: Int = 0
            private set

        private var tempFile: File? = null
        private var lastCutoutResult: CutoutResult? = null

        // Precomputed lookup table for sigmoid contrast curve
        private val contrastLUT = FloatArray(256) { i ->
            val x = i / 255.0f
            val remapped = 1.0f / (1.0f + Math.exp(-12.0 * (x - 0.5)).toFloat())
            remapped.coerceIn(0f, 1f)
        }

        /**
         * Initialize the ONNX sessions and run the image encoder once.
         */
        suspend fun initAndRunEncoder(): Boolean = withContext(Dispatchers.Default) {
            try {
                if (!modelManager.isReady) {
                    printError("CutoutSession: ModelManager is not ready.")
                    return@withContext false
                }

                // 1. Resolve URI and decode original Bitmap
                val uri = if (media.isEncrypted) {
                    val keychainHolder = KeychainHolder(context)
                    tempFile = createDecryptedTempFile(media, keychainHolder)
                    FileProvider.getUriForFile(context, BuildConfig.CONTENT_AUTHORITY, tempFile!!)
                } else {
                    context.resolveShareableUri(media)
                }

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    originalBitmap = BitmapFactory.decodeStream(stream)
                }

                val orig = originalBitmap ?: return@withContext false
                widthOrig = orig.width
                heightOrig = orig.height
                printInfo("CutoutSession: Loaded original image: ${widthOrig}x${heightOrig}")

                // 2. Preprocess SAM image (Aspect-ratio preserved scaling + top-left padding)
                scaleSam = 1024f / maxOf(widthOrig, heightOrig)
                newW = Math.round(widthOrig * scaleSam)
                newH = Math.round(heightOrig * scaleSam)

                val resizedSam = Bitmap.createScaledBitmap(orig, newW, newH, true)
                val pixels = IntArray(newW * newH)
                resizedSam.getPixels(pixels, 0, newW, 0, 0, newW, newH)
                resizedSam.recycle()

                // Fill interleaved float array of size [1024, 1024, 3] with padding initialized to 0.0f
                val inputBuffer = FloatArray(1024 * 1024 * 3)
                for (y in 0 until newH) {
                    for (x in 0 until newW) {
                        val color = pixels[y * newW + x]
                        val r = ((color shr 16) and 0xFF).toFloat()
                        val g = ((color shr 8) and 0xFF).toFloat()
                        val b = (color and 0xFF).toFloat()

                        val idx = (y * 1024 + x) * 3
                        inputBuffer[idx] = r
                        inputBuffer[idx + 1] = g
                        inputBuffer[idx + 2] = b
                    }
                }

                // 3. Initialize sessions
                val cpuOptions = OrtSession.SessionOptions().apply {
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 4))
                }

                printInfo("CutoutSession: Creating OrtSessions...")
                val modelsDir = modelManager.modelsDir
                encoderSession = env.createSession(File(modelsDir, "mobile_sam_image_encoder.onnx").absolutePath, cpuOptions)
                decoderSession = env.createSession(File(modelsDir, "sam_mask_decoder_single.onnx").absolutePath, cpuOptions)

                // 4. Run SAM Encoder
                val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputBuffer), longArrayOf(1024, 1024, 3))
                printInfo("CutoutSession: Running Encoder Session...")
                val outputs = encoderSession!!.run(Collections.singletonMap("input_image", inputTensor))
                imageEmbeddings = outputs.get(0) as OnnxTensor
                inputTensor.close()

                printInfo("CutoutSession: Encoder completed successfully.")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                printError("CutoutSession: Error during initialization: ${e.message}")
                close()
                false
            }
        }

        private fun OrtSession.SessionOptions.cpuOptionsForDecoder(): OrtSession.SessionOptions {
            return this
        }

        /**
         * Run decoder using positive/negative prompt points and return a cropped transparent cutout.
         */
        suspend fun runDecoder(points: List<PromptPoint>): CutoutResult? = withContext(Dispatchers.Default) {
            val decoder = decoderSession ?: return@withContext null
            val embeddings = imageEmbeddings ?: return@withContext null

            try {
                val numPoints = points.size
                if (numPoints == 0) return@withContext null

                // Format coordinates to the 1024x1024 scaled image space
                val scaledPoints = points.map { pt ->
                    PromptPoint(
                        x = pt.x * scaleSam,
                        y = pt.y * scaleSam,
                        isPositive = pt.isPositive
                    )
                }

                // If K=1, pad with a dummy point
                val (coordsArray, labelsArray, finalN) = if (numPoints == 1) {
                    val coords = floatArrayOf(
                        scaledPoints[0].x, scaledPoints[0].y,
                        0.0f, 0.0f
                    )
                    val labels = floatArrayOf(
                        if (scaledPoints[0].isPositive) 1.0f else 0.0f,
                        -1.0f
                    )
                    Triple(coords, labels, 2)
                } else {
                    val coords = FloatArray(numPoints * 2)
                    val labels = FloatArray(numPoints)
                    for (i in 0 until numPoints) {
                        coords[i * 2] = scaledPoints[i].x
                        coords[i * 2 + 1] = scaledPoints[i].y
                        labels[i] = if (scaledPoints[i].isPositive) 1.0f else 0.0f
                    }
                    Triple(coords, labels, numPoints)
                }

                // Create OnnxTensors
                val pointCoordsTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(coordsArray), longArrayOf(1, finalN.toLong(), 2))
                val pointLabelsTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(labelsArray), longArrayOf(1, finalN.toLong()))

                val maskInputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(FloatArray(256 * 256)), longArrayOf(1, 1, 256, 256))
                val hasMaskInputTensor = OnnxTensor.createTensor(env, floatArrayOf(0.0f))
                val origImSizeTensor = OnnxTensor.createTensor(env, floatArrayOf(heightOrig.toFloat(), widthOrig.toFloat()))

                val inputs = mapOf(
                    "image_embeddings" to embeddings,
                    "point_coords" to pointCoordsTensor,
                    "point_labels" to pointLabelsTensor,
                    "mask_input" to maskInputTensor,
                    "has_mask_input" to hasMaskInputTensor,
                    "orig_im_size" to origImSizeTensor
                )

                val result = decoder.run(inputs)
                val masksTensor = result.get(0) as OnnxTensor
                val buffer = masksTensor.floatBuffer

                // Find bounding box of SAM mask (logit > 0.0f)
                var minX = widthOrig
                var maxX = -1
                var minY = heightOrig
                var maxY = -1

                buffer.rewind()
                for (y in 0 until heightOrig) {
                    for (x in 0 until widthOrig) {
                        val logit = buffer.get(y * widthOrig + x)
                        if (logit > 0.0f) {
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                        }
                    }
                }

                // Cleanup input tensors
                pointCoordsTensor.close()
                pointLabelsTensor.close()
                maskInputTensor.close()
                hasMaskInputTensor.close()
                origImSizeTensor.close()
                result.close()

                if (maxX == -1 || maxY == -1) return@withContext null

                val bw = maxX - minX + 1
                val bh = maxY - minY + 1

                val orig = originalBitmap ?: return@withContext null
                val cutoutBitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                val originalPixels = IntArray(bw * bh)
                orig.getPixels(originalPixels, 0, bw, minX, minY, bw, bh)

                // Refinement - Extract binary mask, apply box blur and contrast LUT remapping
                val maskPixels = IntArray(bw * bh)
                for (y in 0 until bh) {
                    for (x in 0 until bw) {
                        val logit = buffer.get((minY + y) * widthOrig + (minX + x))
                        val alpha = if (logit > 0.0f) 255 else 0
                        maskPixels[y * bw + x] = (alpha shl 24) or 0x00FFFFFF
                    }
                }

                refineMask(maskPixels, bw, bh, points, minX, minY)

                val maxDim = maxOf(bw, bh)
                val blurR = Math.round(maxDim * 0.005f).coerceIn(1, 8)
                boxBlur(maskPixels, bw, bh, blurR)

                val cutoutPixels = IntArray(bw * bh)
                for (y in 0 until bh) {
                    for (x in 0 until bw) {
                        val idx = y * bw + x
                        val origColor = originalPixels[idx]
                        val rawAlpha = (maskPixels[idx] shr 24) and 0xFF
                        val remappedAlpha = (contrastLUT[rawAlpha] * 255f).toInt()
                        cutoutPixels[idx] = (remappedAlpha shl 24) or (origColor and 0x00FFFFFF)
                    }
                }
                cutoutBitmap.setPixels(cutoutPixels, 0, bw, 0, 0, bw, bh)

                val cutoutResult = CutoutResult(
                    bitmap = cutoutBitmap,
                    originalBounds = Rect(minX, minY, maxX + 1, maxY + 1)
                )
                lastCutoutResult = cutoutResult
                cutoutResult
            } catch (e: Exception) {
                e.printStackTrace()
                printError("CutoutSession: Error during SAM decoder inference: ${e.message}")
                null
            }
        }

        /**
         * Refinement step is now a quick pass-through of the refined SAM result
         */
        suspend fun finalizeCutout(samBounds: Rect): CutoutResult? = withContext(Dispatchers.Default) {
            lastCutoutResult
        }

        private fun refineMask(mask: IntArray, w: Int, h: Int, points: List<PromptPoint>, minX: Int, minY: Int) {
            val size = w * h
            if (size == 0) return

            // 1. Fill all inner holes (background regions not connected to the outer boundary)
            val visitedBg = BooleanArray(size)
            val stack = IntArray(size)
            var stackPtr = 0

            // Helper to push to stack
            fun pushBg(x: Int, y: Int) {
                val idx = y * w + x
                if (idx in 0 until size && !visitedBg[idx] && (mask[idx] ushr 24) == 0) {
                    visitedBg[idx] = true
                    stack[stackPtr++] = idx
                }
            }

            // Push all boundary pixels that are background
            for (x in 0 until w) {
                pushBg(x, 0)
                pushBg(x, h - 1)
            }
            for (y in 1 until h - 1) {
                pushBg(0, y)
                pushBg(w - 1, y)
            }

            // Run BFS/DFS to mark reachable background
            while (stackPtr > 0) {
                val idx = stack[--stackPtr]
                val x = idx % w
                val y = idx / w

                // 4-connectivity
                if (x > 0) pushBg(x - 1, y)
                if (x < w - 1) pushBg(x + 1, y)
                if (y > 0) pushBg(x, y - 1)
                if (y < h - 1) pushBg(x, y + 1)
            }

            // Fill unreached background (holes)
            for (i in 0 until size) {
                if (!visitedBg[i] && (mask[i] ushr 24) == 0) {
                    mask[i] = 0xFFFFFFFF.toInt() // Fill hole
                }
            }

            // 2. Remove small isolated foreground islands (smaller than 200 pixels and containing no prompt points)
            val visitedFg = BooleanArray(size)
            val fgComponent = IntArray(size)

            // Convert prompt points to crop coordinates
            val cropPoints = points.map { pt ->
                Pair((pt.x - minX).toInt(), (pt.y - minY).toInt())
            }

            for (y in 0 until h) {
                for (x in 0 until w) {
                    val startIdx = y * w + x
                    if ((mask[startIdx] ushr 24) != 0 && !visitedFg[startIdx]) {
                        // Found an unvisited foreground component, flood-fill it
                        var compSize = 0
                        
                        visitedFg[startIdx] = true
                        fgComponent[compSize++] = startIdx

                        var compReadPtr = 0
                        while (compReadPtr < compSize) {
                            val idx = fgComponent[compReadPtr++]
                            val cx = idx % w
                            val cy = idx / w

                            fun checkAndAddFg(nx: Int, ny: Int) {
                                val nidx = ny * w + nx
                                if (nidx in 0 until size && (mask[nidx] ushr 24) != 0 && !visitedFg[nidx]) {
                                    visitedFg[nidx] = true
                                    fgComponent[compSize++] = nidx
                                }
                            }

                            if (cx > 0) checkAndAddFg(cx - 1, cy)
                            if (cx < w - 1) checkAndAddFg(cx + 1, cy)
                            if (cy > 0) checkAndAddFg(cx, cy - 1)
                            if (cy < h - 1) checkAndAddFg(cx, cy + 1)
                        }

                        // Check if component contains any prompt point
                        var containsPrompt = false
                        for (i in 0 until compSize) {
                            val idx = fgComponent[i]
                            val cx = idx % w
                            val cy = idx / w
                            if (cropPoints.any { (px, py) -> px == cx && py == cy }) {
                                containsPrompt = true
                                break
                            }
                        }

                        // If component is small (< 200 pixels) and contains no prompt points, erase it
                        if (compSize < 200 && !containsPrompt) {
                            for (i in 0 until compSize) {
                                mask[fgComponent[i]] = 0x00FFFFFF // Erase to background
                            }
                        }
                    }
                }
            }
        }

        private fun boxBlur(pixels: IntArray, w: Int, h: Int, radius: Int) {
            val temp = IntArray(pixels.size)
            // Horizontal pass
            for (y in 0 until h) {
                var sum = 0
                for (x in -radius..radius) {
                    val color = pixels[y * w + x.coerceIn(0, w - 1)]
                    sum += (color shr 24) and 0xFF
                }
                for (x in 0 until w) {
                    temp[y * w + x] = sum / (2 * radius + 1)
                    val nextX = x + radius + 1
                    val prevX = x - radius
                    val nextColor = pixels[y * w + nextX.coerceIn(0, w - 1)]
                    val prevColor = pixels[y * w + prevX.coerceIn(0, w - 1)]
                    sum += ((nextColor shr 24) and 0xFF) - ((prevColor shr 24) and 0xFF)
                }
            }
            // Vertical pass
            for (x in 0 until w) {
                var sum = 0
                for (y in -radius..radius) {
                    sum += temp[y.coerceIn(0, h - 1) * w + x]
                }
                for (y in 0 until h) {
                    pixels[y * w + x] = (sum / (2 * radius + 1)) shl 24 or 0x00FFFFFF
                    val nextY = y + radius + 1
                    val prevY = y - radius
                    val nextAlpha = temp[nextY.coerceIn(0, h - 1) * w + x]
                    val prevAlpha = temp[prevY.coerceIn(0, h - 1) * w + x]
                    sum += nextAlpha - prevAlpha
                }
            }
        }

        /**
         * Explicitly close sessions and release heavy ONNX memory.
         */
        fun close() {
            printInfo("CutoutSession: Releasing resources...")
            try {
                imageEmbeddings?.close()
                imageEmbeddings = null

                encoderSession?.close()
                encoderSession = null

                decoderSession?.close()
                decoderSession = null

                originalBitmap?.recycle()
                originalBitmap = null

                tempFile?.let {
                    if (it.exists()) it.delete()
                }
                tempFile = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Copy the cutout bitmap to the system clipboard as a transparent PNG.
     */
    suspend fun copyToClipboard(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, "cutout_${System.currentTimeMillis()}.png")
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(context, BuildConfig.CONTENT_AUTHORITY, cacheFile)
            withContext<Unit>(Dispatchers.Main) {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newUri(context.contentResolver, "Cutout Object", uri)
                clip.description.extras = PersistableBundle().apply {
                    putString("android.content.extra.IS_SENSITIVE", "false")
                }
                clipboardManager.setPrimaryClip(clip)
                if (!SdkCompat.showsClipboardConfirmation) {
                    Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext<Unit>(Dispatchers.Main) {
                Toast.makeText(context, "Failed to copy cutout object", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Share the cutout bitmap via system share sheet.
     */
    suspend fun shareCutout(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, "cutout_${System.currentTimeMillis()}.png")
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(context, BuildConfig.CONTENT_AUTHORITY, cacheFile)
            withContext<Unit>(Dispatchers.Main) {
                val intent = ShareCompat.IntentBuilder(context)
                    .setType("image/png")
                    .addStream(uri)
                    .createChooserIntent()
                    .apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext<Unit>(Dispatchers.Main) {
                Toast.makeText(context, "Failed to share cutout object", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Save the cutout bitmap as a transparent PNG file under Pictures/Cutouts.
     */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            val savedUri = context.contentResolver.saveImage(
                bitmap = bitmap,
                format = Bitmap.CompressFormat.PNG,
                mimeType = "image/png",
                relativePath = "${Environment.DIRECTORY_PICTURES}/Cutouts",
                displayName = "cutout_${System.currentTimeMillis()}"
            )
            withContext<Unit>(Dispatchers.Main) {
                if (savedUri != null) {
                    Toast.makeText(context, "Saved cutout to Pictures/Cutouts", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save cutout object", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext<Unit>(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save cutout object", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
