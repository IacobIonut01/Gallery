/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.core.graphics.scale
import com.dot.gallery.feature_node.presentation.util.printWarning
import java.nio.FloatBuffer
import java.util.Collections

/**
 * A single detected face: bounding box in normalized [0,1] image coordinates plus the detector
 * confidence and (optionally) a recognition embedding.
 */
data class DetectedFaceBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float,
    val embedding: FloatArray? = null
) {
    val rectF get() = RectF(left, top, right, bottom)
}

/**
 * On-device face detection ([ModelGroup.FACE_DETECT], UltraFace RFB-320) and face embedding
 * ([ModelGroup.FACE_RECOGNITION], ArcFace 112x112 -> 512-d). Powers the editor's auto face-blur
 * and the Person-grouping indexer. Detection and recognition are independent so auto-blur works
 * with only the detector installed.
 *
 * Not thread-safe; create one instance per worker/session and [close] it when done.
 */
class FaceHelper(private val modelManager: ModelManager) {

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()

    private var detectSession: OrtSession? = null
    private var recogSession: OrtSession? = null

    val isDetectionAvailable: Boolean get() = modelManager.isReady(ModelGroup.FACE_DETECT)
    val isRecognitionAvailable: Boolean get() = modelManager.isReady(ModelGroup.FACE_RECOGNITION)

    private fun options() = OrtSession.SessionOptions().apply {
        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 4))
    }

    private fun detector(): OrtSession {
        detectSession?.let { return it }
        if (!isDetectionAvailable) throw ModelsNotAvailableException()
        val file = modelManager.getModelFile("version-RFB-320.onnx")
        return ortEnv.createSession(file.absolutePath, options()).also { detectSession = it }
    }

    private fun recognizer(): OrtSession {
        recogSession?.let { return it }
        if (!isRecognitionAvailable) throw ModelsNotAvailableException()
        val file = modelManager.getModelFile("arcface.onnx")
        return ortEnv.createSession(file.absolutePath, options()).also { recogSession = it }
    }

    /**
     * Detect faces in [bitmap]. Returns boxes in normalized [0,1] coordinates, filtered by
     * [scoreThreshold] and de-duplicated with non-max suppression.
     */
    fun detect(bitmap: Bitmap, scoreThreshold: Float = 0.7f): List<DetectedFaceBox> {
        val session = detector()
        val resized = bitmap.scale(INPUT_W, INPUT_H)

        val buffer = FloatBuffer.allocate(3 * INPUT_H * INPUT_W)
        // NCHW, RGB, (px - 127) / 128
        for (c in 0 until 3) {
            for (y in 0 until INPUT_H) {
                for (x in 0 until INPUT_W) {
                    val px = resized.getPixel(x, y)
                    val v = when (c) {
                        0 -> (px shr 16) and 0xFF // R
                        1 -> (px shr 8) and 0xFF  // G
                        else -> px and 0xFF       // B
                    }
                    buffer.put((v - 127f) / 128f)
                }
            }
        }
        buffer.rewind()
        if (resized != bitmap) resized.recycle()

        val inputName = session.inputNames.iterator().next()
        val tensor = OnnxTensor.createTensor(
            ortEnv, buffer, longArrayOf(1, 3, INPUT_H.toLong(), INPUT_W.toLong())
        )
        val results = tensor.use {
            session.run(Collections.singletonMap(inputName, tensor))
        }

        results.use {
            // Identify outputs by trailing dimension: scores => 2, boxes => 4.
            var scores: Array<FloatArray>? = null
            var boxes: Array<FloatArray>? = null
            for (i in 0 until results.size()) {
                @Suppress("UNCHECKED_CAST")
                val arr = (results[i].value as Array<Array<FloatArray>>)[0]
                val lastDim = arr.firstOrNull()?.size ?: 0
                if (lastDim == 2) scores = arr else if (lastDim == 4) boxes = arr
            }
            val s = scores ?: return emptyList()
            val b = boxes ?: return emptyList()

            val candidates = ArrayList<DetectedFaceBox>()
            for (i in s.indices) {
                val faceProb = s[i][1]
                if (faceProb >= scoreThreshold) {
                    val box = b[i]
                    candidates.add(
                        DetectedFaceBox(
                            left = box[0].coerceIn(0f, 1f),
                            top = box[1].coerceIn(0f, 1f),
                            right = box[2].coerceIn(0f, 1f),
                            bottom = box[3].coerceIn(0f, 1f),
                            confidence = faceProb
                        )
                    )
                }
            }
            return nonMaxSuppression(candidates, 0.3f)
        }
    }

    /**
     * Compute an L2-normalized embedding for a face region [rect] (normalized coords) of [bitmap].
     * Returns null when the recognition model is unavailable or the crop is degenerate.
     */
    fun embed(bitmap: Bitmap, rect: RectF): FloatArray? {
        if (!isRecognitionAvailable) return null
        val left = (rect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (rect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (rect.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (rect.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        val w = right - left
        val h = bottom - top
        if (w < 2 || h < 2) return null

        return try {
            val crop = Bitmap.createBitmap(bitmap, left, top, w, h)
            val face = crop.scale(RECOG_SIZE, RECOG_SIZE)
            if (crop != face) crop.recycle()

            // ArcFace (garavv/arc.onnx): NHWC 1x112x112x3, RGB, (px - 127.5) / 127.5
            val buffer = FloatBuffer.allocate(RECOG_SIZE * RECOG_SIZE * 3)
            for (y in 0 until RECOG_SIZE) {
                for (x in 0 until RECOG_SIZE) {
                    val px = face.getPixel(x, y)
                    buffer.put((((px shr 16) and 0xFF) - 127.5f) / 127.5f)
                    buffer.put((((px shr 8) and 0xFF) - 127.5f) / 127.5f)
                    buffer.put(((px and 0xFF) - 127.5f) / 127.5f)
                }
            }
            buffer.rewind()
            face.recycle()

            val session = recognizer()
            val inputName = session.inputNames.iterator().next()
            val tensor = OnnxTensor.createTensor(
                ortEnv, buffer, longArrayOf(1, RECOG_SIZE.toLong(), RECOG_SIZE.toLong(), 3)
            )
            val out = tensor.use { session.run(Collections.singletonMap(inputName, tensor)) }
            out.use {
                @Suppress("UNCHECKED_CAST")
                val raw = (out[0].value as Array<FloatArray>)[0]
                l2Normalize(raw)
            }
        } catch (e: Exception) {
            printWarning("FaceHelper.embed failed: ${e.message}")
            null
        }
    }

    fun close() {
        runCatching { detectSession?.close() }
        runCatching { recogSession?.close() }
        detectSession = null
        recogSession = null
    }

    companion object {
        private const val INPUT_W = 320
        private const val INPUT_H = 240
        private const val RECOG_SIZE = 112

        fun l2Normalize(v: FloatArray): FloatArray {
            var sum = 0f
            for (x in v) sum += x * x
            val norm = kotlin.math.sqrt(sum).coerceAtLeast(1e-10f)
            return FloatArray(v.size) { v[it] / norm }
        }

        /** Cosine similarity of two L2-normalized vectors (== dot product). */
        fun cosine(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return -1f
            var dot = 0f
            for (i in a.indices) dot += a[i] * b[i]
            return dot
        }

        private fun iou(a: DetectedFaceBox, b: DetectedFaceBox): Float {
            val ix = maxOf(a.left, b.left)
            val iy = maxOf(a.top, b.top)
            val ax = minOf(a.right, b.right)
            val ay = minOf(a.bottom, b.bottom)
            val iw = (ax - ix).coerceAtLeast(0f)
            val ih = (ay - iy).coerceAtLeast(0f)
            val inter = iw * ih
            val areaA = (a.right - a.left) * (a.bottom - a.top)
            val areaB = (b.right - b.left) * (b.bottom - b.top)
            val union = areaA + areaB - inter
            return if (union <= 0f) 0f else inter / union
        }

        private fun nonMaxSuppression(
            boxes: List<DetectedFaceBox>,
            iouThreshold: Float
        ): List<DetectedFaceBox> {
            val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()
            val kept = ArrayList<DetectedFaceBox>()
            while (sorted.isNotEmpty()) {
                val best = sorted.removeAt(0)
                kept.add(best)
                sorted.removeAll { iou(best, it) > iouThreshold }
            }
            return kept
        }
    }
}
