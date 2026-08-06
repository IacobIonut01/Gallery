package com.dot.gallery.feature_node.domain.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.dot.gallery.feature_node.presentation.util.printDebug
import com.dot.gallery.feature_node.presentation.util.printWarning
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.xmp.XmpDirectory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * Describes the embedded video inside a Motion Photo / Microvideo file.
 *
 * @param videoOffset Byte offset from **end** of file where the MP4 stream starts.
 * @param presentationTimestampUs The "favourite shot" / key-frame timestamp in µs.
 *        A value of -1 means the field was absent.
 */
data class MotionPhotoInfo(
    val videoOffset: Long,
    val presentationTimestampUs: Long = -1L
)

object MotionPhotoHelper {

    // --- XMP property keys (Google Camera / Samsung) ---------------------------------

    // Motion Photo v2/v3
    private const val KEY_MOTION_PHOTO = "GCamera:MotionPhoto"
    private const val KEY_MOTION_PHOTO_VERSION = "GCamera:MotionPhotoVersion"
    private const val KEY_MOTION_PHOTO_OFFSET = "GCamera:MotionPhotoVideoOffset"
    private const val KEY_MOTION_PHOTO_PTS = "GCamera:MotionPhotoPresentationTimestampUs"

    // Micro-video v1 (deprecated, still common)
    private const val KEY_MICRO_VIDEO = "GCamera:MicroVideo"
    private const val KEY_MICRO_VIDEO_OFFSET = "GCamera:MicroVideoOffset"
    private const val KEY_MICRO_VIDEO_PTS = "GCamera:MicroVideoPresentationTimestampUs"

    // Container-based key fragments for dynamic lookup
    private const val SEMANTIC_MOTION_PHOTO = "MotionPhoto"
    private const val ITEM_SEMANTIC_SUFFIX = "/Item:Semantic"
    private const val ITEM_LENGTH_SUFFIX = "/Item:Length"
    private const val ITEM_PADDING_SUFFIX = "/Item:Padding"

    // Samsung uses a binary marker appended to the file
    private val SAMSUNG_MARKER = "MotionPhoto_Data".toByteArray(Charsets.US_ASCII)

    // Samsung markers are scanned with constant memory so large embedded clips remain detectable.

    // ---------------------------------------------------------------------------------

    /**
     * Parse XMP metadata from an image [uri] and return [MotionPhotoInfo] if the file
     * is a recognised Motion Photo / Microvideo.  Returns `null` otherwise.
     */
    fun parseInfo(context: Context, uri: Uri): MotionPhotoInfo? {
        return try {
            // Defense-in-depth: metadata-extractor's TiffReader loads the whole file into memory, so
            // running it on a huge non-photo container (e.g. a 16-bit TIFF) OOMs. Motion Photos are
            // only ever JPEG/HEIC, so bail out unless the header actually looks like one.
            if (!looksLikeJpegOrHeic(context, uri)) {
                printDebug("MotionPhoto: header is not JPEG/HEIC, skipping metadata read")
                return null
            }
            // First try XMP-based detection
            val xmpResult = context.contentResolver.openInputStream(uri)?.use { stream ->
                val metadata = ImageMetadataReader.readMetadata(stream)
                val xmpDirs = metadata.getDirectoriesOfType(XmpDirectory::class.java)

                // Collect all XMP properties into a flat map for easier lookup
                val props = mutableMapOf<String, String>()
                xmpDirs.forEach { dir ->
                    dir.xmpProperties.forEach { (k, v) -> props[k] = v }
                }

                resolveInfo(props)
            }

            if (xmpResult != null) return xmpResult

            // Fallback: scan for the Samsung binary marker without loading the source into memory.
            printDebug("MotionPhoto: XMP detection found nothing, trying Samsung marker")
            val offset = findSamsungMarkerOffset(context, uri) ?: return null
            offset.let {
                printDebug("MotionPhoto: Samsung marker found at offset $offset")
                MotionPhotoInfo(offset)
            }
        } catch (e: Throwable) {
            // Throwable (not just Exception): metadata-extractor can OOM on malformed/huge files, and
            // OutOfMemoryError is an Error that would otherwise crash the app.
            printWarning("MotionPhotoHelper.parseInfo failed: ${e.message}")
            null
        }
    }

    fun resolveInfo(properties: Map<String, String>): MotionPhotoInfo? {
        if (properties[KEY_MOTION_PHOTO] != "1" && properties[KEY_MICRO_VIDEO] != "1") return null

        var videoOffset = properties[KEY_MOTION_PHOTO_OFFSET]
            ?.toLongOrNull()
            ?.takeIf { it > 0L }

        if (videoOffset == null) {
            val motionEntry = properties.entries.firstOrNull { (key, value) ->
                key.endsWith(ITEM_SEMANTIC_SUFFIX) && value == SEMANTIC_MOTION_PHOTO
            }
            if (motionEntry != null) {
                val prefix = motionEntry.key.removeSuffix(ITEM_SEMANTIC_SUFFIX)
                val length = properties["$prefix$ITEM_LENGTH_SUFFIX"]?.toLongOrNull()
                val padding = properties["$prefix$ITEM_PADDING_SUFFIX"]?.toLongOrNull() ?: 0L
                if (length != null && length > 0L && padding >= 0L && length <= Long.MAX_VALUE - padding) {
                    videoOffset = length + padding
                }
            }
        }

        if (videoOffset == null) {
            videoOffset = properties[KEY_MICRO_VIDEO_OFFSET]
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        }

        val presentationTimestampUs = properties[KEY_MOTION_PHOTO_PTS]
            ?.toLongOrNull()
            ?.takeIf { it >= 0L }
            ?: properties[KEY_MICRO_VIDEO_PTS]?.toLongOrNull()?.takeIf { it >= 0L }
            ?: -1L
        return videoOffset?.let { MotionPhotoInfo(it, presentationTimestampUs) }
    }

    fun videoStart(fileSize: Long, videoOffset: Long): Long? =
        if (fileSize > 0L && videoOffset in 1..fileSize) fileSize - videoOffset else null

    fun hasMp4Ftyp(header: ByteArray, bytesRead: Int = header.size): Boolean =
        bytesRead >= 8 && header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
            header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()

    /**
     * Cheaply sniff the leading bytes to confirm the file is a JPEG or an ISO-BMFF (HEIC/HEIF)
     * container before handing it to the greedy metadata reader. Reads only a few bytes.
     */
    private fun looksLikeJpegOrHeic(context: Context, uri: Uri): Boolean {
        val header = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buf = ByteArray(12)
                var read = 0
                while (read < buf.size) {
                    val n = stream.read(buf, read, buf.size - read)
                    if (n < 0) break
                    read += n
                }
                if (read < 12) return false
                buf
            } ?: return false
        } catch (_: Throwable) {
            return false
        }
        // JPEG: FF D8 FF
        if ((header[0].toInt() and 0xFF) == 0xFF &&
            (header[1].toInt() and 0xFF) == 0xD8 &&
            (header[2].toInt() and 0xFF) == 0xFF
        ) return true
        // ISO-BMFF (HEIC/HEIF): bytes 4..7 == "ftyp"
        return header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()
    }

    private fun findSamsungMarkerOffset(context: Context, uri: Uri): Long? =
        context.contentResolver.openInputStream(uri)?.use { input ->
            val scan = scanSamsungMarker(input)
            scan.lastMarkerEnd?.let { scan.totalBytes - it }
        }

    /**
     * Attempts to find the Samsung MotionPhoto_Data marker inside the file
     * and returns the offset of the video data after the marker.
     */
    fun findSamsungMarkerOffset(bytes: ByteArray): Long? {
        val marker = SAMSUNG_MARKER
        val markerLen = marker.size
        if (bytes.size < markerLen + 4) return null

        // Search backwards from end for better perf (marker is near EOF)
        var i = bytes.size - markerLen
        while (i >= 0) {
            if (bytes[i] == marker[0]) {
                var match = true
                for (j in 1 until markerLen) {
                    if (bytes[i + j] != marker[j]) { match = false; break }
                }
                if (match) {
                    val videoStart = i + markerLen
                    return (bytes.size - videoStart).toLong()
                }
            }
            i--
        }
        return null
    }

    /**
     * Extract the embedded MP4 video from a Motion Photo file and write it to
     * a temporary file in [context]'s cache directory.
     *
     * @return The temp [File] containing the MP4 video, or `null` on failure.
     */
    suspend fun extractVideo(
        context: Context,
        uri: Uri,
        info: MotionPhotoInfo,
        outputDirectory: File = File(context.cacheDir, "frame_picker_sources"),
    ): File? = withContext(Dispatchers.IO) {
        outputDirectory.mkdirs()
        val partFile = File(outputDirectory, "${UUID.randomUUID()}.part")
        val outputFile = File(outputDirectory, "${partFile.nameWithoutExtension}.mp4")
        try {
            val fileSize = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
                ?.takeIf { it > 0L }
                ?: copySourceToSeekableFile(context, uri, outputDirectory)?.let { sourceCopy ->
                    try {
                        return@withContext extractVideoFromFile(sourceCopy, info, outputDirectory)
                    } finally {
                        sourceCopy.delete()
                    }
                }
                ?: return@withContext null
            val initialStart = videoStart(fileSize, info.videoOffset)
            val resolvedStart = initialStart?.takeIf {
                sourceHasFtyp(context, uri, it)
            } ?: findSamsungVideoStart(context, uri, fileSize)
                ?: return@withContext null
            val byteCount = fileSize - resolvedStart
            if (byteCount <= 8L) return@withContext null

            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).channel.use { input ->
                    FileOutputStream(partFile).channel.use { output ->
                        input.position(resolvedStart)
                        var copied = 0L
                        while (copied < byteCount) {
                            coroutineContext.ensureActive()
                            val count = input.transferTo(input.position(), byteCount - copied, output)
                            if (count <= 0L) break
                            input.position(input.position() + count)
                            copied += count
                        }
                        if (copied != byteCount) throw java.io.IOException("Incomplete Motion Photo extraction")
                    }
                }
            } ?: return@withContext null
            if (!partFile.renameTo(outputFile)) throw java.io.IOException("Unable to publish extracted video")
            outputFile
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            printWarning("MotionPhoto: extraction failed: ${e.message}")
            partFile.delete()
            outputFile.delete()
            null
        }
    }

    suspend fun extractVideoFromFile(
        sourceFile: File,
        info: MotionPhotoInfo,
        outputDirectory: File,
    ): File? = withContext(Dispatchers.IO) {
        outputDirectory.mkdirs()
        val fileSize = sourceFile.length()
        val initialStart = videoStart(fileSize, info.videoOffset)
        val resolvedStart = initialStart?.takeIf { sourceHasFtyp(sourceFile, it) }
            ?: findSamsungVideoStart(sourceFile)
            ?: return@withContext null
        val partFile = File(outputDirectory, "${UUID.randomUUID()}.part")
        val outputFile = File(outputDirectory, "${partFile.nameWithoutExtension}.mp4")
        try {
            FileInputStream(sourceFile).channel.use { input ->
                FileOutputStream(partFile).channel.use { output ->
                    input.position(resolvedStart)
                    val byteCount = fileSize - resolvedStart
                    var copied = 0L
                    while (copied < byteCount) {
                        coroutineContext.ensureActive()
                        val count = input.transferTo(input.position(), byteCount - copied, output)
                        if (count <= 0L) break
                        input.position(input.position() + count)
                        copied += count
                    }
                    if (copied != byteCount) throw java.io.IOException("Incomplete Motion Photo extraction")
                }
            }
            if (!partFile.renameTo(outputFile)) throw java.io.IOException("Unable to publish extracted video")
            outputFile
        } catch (e: Exception) {
            partFile.delete()
            outputFile.delete()
            if (e is CancellationException) throw e
            printWarning("MotionPhoto: file extraction failed: ${e.message}")
            null
        }
    }

    private fun copySourceToSeekableFile(context: Context, uri: Uri, outputDirectory: File): File? {
        val partFile = File(outputDirectory, "${UUID.randomUUID()}.source.part")
        val sourceFile = File(outputDirectory, "${partFile.nameWithoutExtension}.source")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(partFile).use(input::copyTo)
            } ?: return null
            if (!partFile.renameTo(sourceFile)) throw java.io.IOException("Unable to publish source copy")
            sourceFile
        } catch (_: Exception) {
            partFile.delete()
            sourceFile.delete()
            null
        }
    }

    private fun sourceHasFtyp(context: Context, uri: Uri, start: Long): Boolean {
        val header = ByteArray(12)
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                    channel.position(start)
                    val read = channel.read(ByteBuffer.wrap(header))
                    hasMp4Ftyp(header, read)
                }
            } ?: false
        } catch (_: Exception) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                if (!input.skipFully(start)) return@use false
                hasMp4Ftyp(header, input.read(header))
            } ?: false
        }
    }

    private fun sourceHasFtyp(sourceFile: File, start: Long): Boolean {
        val header = ByteArray(12)
        return try {
            FileInputStream(sourceFile).channel.use { channel ->
                channel.position(start)
                hasMp4Ftyp(header, channel.read(ByteBuffer.wrap(header)))
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun findSamsungVideoStart(context: Context, uri: Uri, fileSize: Long): Long? =
        context.contentResolver.openInputStream(uri)?.use { input ->
            scanSamsungMarker(input).lastMarkerEnd?.takeIf { sourceHasFtyp(context, uri, it) }
        }?.takeIf { it in 0 until fileSize }

    private fun findSamsungVideoStart(sourceFile: File): Long? =
        sourceFile.inputStream().use { input ->
            scanSamsungMarker(input).lastMarkerEnd?.takeIf { sourceHasFtyp(sourceFile, it) }
        }

    private data class SamsungMarkerScan(val totalBytes: Long, val lastMarkerEnd: Long?)

    private fun scanSamsungMarker(input: InputStream): SamsungMarkerScan {
        val marker = SAMSUNG_MARKER
        val window = ByteArray(marker.size)
        var total = 0L
        var count = 0
        var cursor = 0
        var lastEnd: Long? = null
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            for (bufferIndex in 0 until read) {
                window[cursor] = buffer[bufferIndex]
                cursor = (cursor + 1) % marker.size
                count = (count + 1).coerceAtMost(marker.size)
                total++
                if (count == marker.size) {
                    var matches = true
                    for (index in marker.indices) {
                        if (window[(cursor + index) % marker.size] != marker[index]) {
                            matches = false
                            break
                        }
                    }
                    if (matches) lastEnd = total
                }
            }
        }
        return SamsungMarkerScan(total, lastEnd)
    }

    private fun InputStream.skipFully(byteCount: Long): Boolean {
        var remaining = byteCount
        while (remaining > 0L) {
            val skipped = skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else if (read() >= 0) {
                remaining--
            } else {
                return false
            }
        }
        return true
    }

    /**
     * Extract [numFrames] evenly-spaced thumbnail frames from the given video [file].
     * Returns the list of bitmaps (may be fewer than requested if extraction fails
     * for some timestamps).
     */
    suspend fun extractFrames(
        file: File,
        numFrames: Int = 10
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val durationUs = (retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L) * 1000L // ms → µs
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 1
            val scale = minOf(1f, 512f / maxOf(width, height))
            val targetWidth = (width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (height * scale).toInt().coerceAtLeast(1)

            if (durationUs <= 0 || numFrames <= 0) return@withContext frames

            val intervalUs = durationUs / numFrames
            for (i in 0 until numFrames) {
                coroutineContext.ensureActive()
                val timeUs = i * intervalUs + intervalUs / 2
                val bitmap = retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    targetWidth,
                    targetHeight,
                )
                if (bitmap != null) {
                    frames.add(bitmap)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            printWarning("MotionPhoto: frame extraction failed: ${e.message}")
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        frames
    }

    /**
     * Extract the embedded video from the Motion Photo at [uri] and save it to the
     * gallery as a standalone MP4, mirroring Google Photos' "Save as video" export.
     *
     * The video is written into [relativeDir] (a [MediaStore] relative path such as
     * `Movies` or `DCIM/Camera`) with the file name derived from [sourceLabel].
     *
     * @return The content [Uri] of the newly saved video, or `null` if the source was
     *         not a Motion Photo or the export failed.
     */
    suspend fun saveVideoToGallery(
        context: Context,
        uri: Uri,
        sourceLabel: String,
        relativeDir: String = Environment.DIRECTORY_MOVIES
    ): Uri? = withContext(Dispatchers.IO) {
        val info = parseInfo(context, uri) ?: return@withContext null
        val tmpFile = extractVideo(context, uri, info) ?: return@withContext null
        var insertedUri: Uri? = null
        try {
            val baseName = sourceLabel.substringBeforeLast('.', sourceLabel)
            val displayName = "${baseName}_motion.mp4"

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val insertUri = resolver.insert(collection, values) ?: return@withContext null
            insertedUri = insertUri

            resolver.openOutputStream(insertUri)?.use { output ->
                tmpFile.inputStream().use { input -> input.copyTo(output) }
            } ?: run {
                resolver.delete(insertUri, null, null)
                return@withContext null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                if (resolver.update(insertUri, values, null, null) <= 0) {
                    throw java.io.IOException("Unable to publish extracted video")
                }
            }

            printDebug("MotionPhoto: saved extracted video to $insertUri")
            insertedUri = null
            insertUri
        } catch (e: Exception) {
            insertedUri?.let { context.contentResolver.delete(it, null, null) }
            printWarning("MotionPhoto: saveVideoToGallery failed: ${e.message}")
            null
        } finally {
            tmpFile.delete()
        }
    }
}
