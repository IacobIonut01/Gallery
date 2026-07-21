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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

    // The Samsung marker and its trailing video live at the very end of the file. Only scan a
    // bounded tail instead of reading the whole file into memory: a large photo (e.g. 150+ MP)
    // can be well over 100 MB and reading it all triggers an OutOfMemoryError. Real Samsung
    // motion-photo clips are a few MB, so this window comfortably covers them.
    private const val SAMSUNG_TAIL_SCAN_BYTES = 16 * 1024 * 1024

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
                printDebug("MotionPhoto: header is not JPEG/HEIC, skipping metadata read for $uri")
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

                printDebug("MotionPhoto: XMP props for $uri: ${
                    props.filter { it.key.contains("Camera", true) || it.key.contains("Container", true) || it.key.contains("Micro", true) || it.key.contains("Motion", true) }
                }")

                // 1. Try Motion Photo v2/v3 offset
                val motionPhotoFlag = props[KEY_MOTION_PHOTO]
                val microVideoFlag = props[KEY_MICRO_VIDEO]

                if (motionPhotoFlag != "1" && microVideoFlag != "1") return@use null

                var videoOffset: Long? = null
                var pts: Long = -1L

                // v2/v3: explicit MotionPhotoVideoOffset
                props[KEY_MOTION_PHOTO_OFFSET]?.toLongOrNull()?.let { offset ->
                    if (offset > 0) videoOffset = offset
                }

                // v2/v3: Container-based offset – find the item whose Semantic is "MotionPhoto"
                if (videoOffset == null) {
                    // Find the key prefix for the MotionPhoto container item
                    val motionEntry = props.entries.firstOrNull { (k, v) ->
                        k.endsWith(ITEM_SEMANTIC_SUFFIX) && v == SEMANTIC_MOTION_PHOTO
                    }
                    if (motionEntry != null) {
                        val prefix = motionEntry.key.removeSuffix(ITEM_SEMANTIC_SUFFIX)
                        val length = props["$prefix$ITEM_LENGTH_SUFFIX"]?.toLongOrNull()
                        val padding = props["$prefix$ITEM_PADDING_SUFFIX"]?.toLongOrNull() ?: 0L
                        if (length != null && length > 0) {
                            videoOffset = length + padding
                            printDebug("MotionPhoto: Container item found at prefix=$prefix, length=$length, padding=$padding")
                        }
                    }
                }

                // v1: MicroVideoOffset
                if (videoOffset == null) {
                    props[KEY_MICRO_VIDEO_OFFSET]?.toLongOrNull()?.let { offset ->
                        if (offset > 0) videoOffset = offset
                    }
                }

                // Presentation timestamp
                props[KEY_MOTION_PHOTO_PTS]?.toLongOrNull()?.let { pts = it }
                if (pts < 0) {
                    props[KEY_MICRO_VIDEO_PTS]?.toLongOrNull()?.let { pts = it }
                }

                videoOffset?.let { MotionPhotoInfo(it, pts) }
            }

            if (xmpResult != null) return xmpResult

            // Fallback: try Samsung binary marker detection. Only scan a bounded tail of the file
            // (the marker + video are appended at EOF) to avoid loading huge photos into memory.
            printDebug("MotionPhoto: XMP detection found nothing, trying Samsung marker for $uri")
            val tail = readFileTail(context, uri, SAMSUNG_TAIL_SCAN_BYTES) ?: return null
            findSamsungMarkerOffset(tail)?.let { offset ->
                printDebug("MotionPhoto: Samsung marker found at offset $offset for $uri")
                MotionPhotoInfo(offset)
            }
        } catch (e: Throwable) {
            // Throwable (not just Exception): metadata-extractor can OOM on malformed/huge files, and
            // OutOfMemoryError is an Error that would otherwise crash the app.
            printWarning("MotionPhotoHelper.parseInfo failed: ${e.message}")
            null
        }
    }

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

    /**
     * Reads up to [maxBytes] from the **end** of the file at [uri] without loading the whole file
     * into memory. Returns the tail bytes (ending at EOF) or the entire file if it is smaller than
     * [maxBytes]. Returns `null` if the file length cannot be determined (in which case the caller
     * should skip the scan rather than risk an OutOfMemoryError).
     */
    private fun readFileTail(context: Context, uri: Uri, maxBytes: Int): ByteArray? {
        val length = try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
        } catch (e: Exception) {
            null
        }
        if (length == null || length <= 0L) return null

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            val skipTarget = (length - maxBytes).coerceAtLeast(0L)
            var skipped = 0L
            while (skipped < skipTarget) {
                val s = stream.skip(skipTarget - skipped)
                if (s <= 0L) break
                skipped += s
            }
            if (skipped < skipTarget) return@use null
            stream.readBytes()
        }
    }

    /**
     * Attempts to find the Samsung MotionPhoto_Data marker inside the file
     * and returns the offset of the video data after the marker.
     */
    private fun findSamsungMarkerOffset(bytes: ByteArray): Long? {
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
        info: MotionPhotoInfo
    ): File? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null

            var offset = info.videoOffset

            // Sanity-check: if offset is bigger than file, try Samsung marker fallback
            if (offset <= 0 || offset > bytes.size) {
                offset = findSamsungMarkerOffset(bytes) ?: return@withContext null
            }

            val videoStart = (bytes.size - offset).toInt()
            if (videoStart < 0 || videoStart >= bytes.size) {
                printWarning("MotionPhoto: invalid video start $videoStart for file size ${bytes.size}")
                return@withContext null
            }

            val videoBytes = bytes.copyOfRange(videoStart, bytes.size)

            // Quick sanity: MP4 files start with 'ftyp' box (at byte 4)
            if (videoBytes.size > 8) {
                val ftyp = String(videoBytes, 4, 4, Charsets.US_ASCII)
                if (ftyp != "ftyp") {
                    printWarning("MotionPhoto: extracted data does not start with ftyp box (got '$ftyp'), trying Samsung marker fallback")
                    // Fallback to Samsung marker
                    val samsungOffset = findSamsungMarkerOffset(bytes)
                    if (samsungOffset != null && samsungOffset != offset) {
                        val samsungStart = (bytes.size - samsungOffset).toInt()
                        if (samsungStart in 0 until bytes.size) {
                            val samsungVideoBytes = bytes.copyOfRange(samsungStart, bytes.size)
                            if (samsungVideoBytes.size > 8) {
                                val samsungFtyp = String(samsungVideoBytes, 4, 4, Charsets.US_ASCII)
                                if (samsungFtyp == "ftyp") {
                                    val tmpFile = File(context.cacheDir, "motion_photo_${System.currentTimeMillis()}.mp4")
                                    tmpFile.writeBytes(samsungVideoBytes)
                                    printDebug("MotionPhoto: extracted ${samsungVideoBytes.size} bytes (Samsung fallback) to ${tmpFile.absolutePath}")
                                    return@withContext tmpFile
                                }
                            }
                        }
                    }
                    // Still no luck, write what we have
                }
            }

            val tmpFile = File(context.cacheDir, "motion_photo_${System.currentTimeMillis()}.mp4")
            tmpFile.writeBytes(videoBytes)
            printDebug("MotionPhoto: extracted ${videoBytes.size} bytes to ${tmpFile.absolutePath}")
            tmpFile
        } catch (e: Exception) {
            printWarning("MotionPhoto: extraction failed: ${e.message}")
            null
        }
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

            if (durationUs <= 0 || numFrames <= 0) return@withContext frames

            val intervalUs = durationUs / numFrames
            for (i in 0 until numFrames) {
                val timeUs = i * intervalUs + intervalUs / 2
                val bitmap = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (bitmap != null) {
                    frames.add(bitmap)
                }
            }
        } catch (e: Exception) {
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

            resolver.openOutputStream(insertUri)?.use { output ->
                tmpFile.inputStream().use { input -> input.copyTo(output) }
            } ?: run {
                resolver.delete(insertUri, null, null)
                return@withContext null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(insertUri, values, null, null)
            }

            printDebug("MotionPhoto: saved extracted video to $insertUri")
            insertUri
        } catch (e: Exception) {
            printWarning("MotionPhoto: saveVideoToGallery failed: ${e.message}")
            null
        } finally {
            tmpFile.delete()
        }
    }
}
