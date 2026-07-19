/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.sandbox

import android.app.Service
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.mov.media.QuickTimeVideoDirectory
import com.drew.metadata.mp4.media.Mp4VideoDirectory
import com.drew.metadata.xmp.XmpDirectory
import java.io.ByteArrayInputStream
import java.io.FileInputStream

/**
 * Isolated-process service for metadata parsing.
 *
 * Runs under a fake UID with no permissions, no app data, and no network.
 * If a malformed file exploits a bug in metadata-extractor or MediaMetadataRetriever,
 * the attacker cannot reach the user's media library or network.
 *
 * Communication is via [Messenger] IPC:
 * - Client sends a [Message] with [MSG_PARSE_IMAGE], [MSG_PARSE_VIDEO], or [MSG_PARSE_RAW_METADATA]
 * - Data is passed as a [ParcelFileDescriptor] in the message Bundle
 * - Service replies via [Message.replyTo] with a result Bundle
 */
class IsolatedMetadataService : Service() {

    private lateinit var messenger: Messenger

    override fun onCreate() {
        super.onCreate()
        messenger = Messenger(IncomingHandler(Looper.getMainLooper()))
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    private inner class IncomingHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            val replyTo = msg.replyTo ?: return
            val data = msg.data
            data.classLoader = ParcelFileDescriptor::class.java.classLoader

            val reply = Message.obtain()
            reply.what = msg.what

            try {
                reply.data = when (msg.what) {
                    MSG_PARSE_IMAGE -> parseImageMetadata(data)
                    MSG_PARSE_VIDEO -> parseVideoMetadata(data)
                    MSG_PARSE_RAW_METADATA -> parseRawMetadata(data)
                    else -> Bundle().apply { putBoolean(KEY_ERROR, true) }
                }
            } catch (e: Exception) {
                reply.data = Bundle().apply {
                    putBoolean(KEY_ERROR, true)
                    putString(KEY_ERROR_MESSAGE, e.message)
                }
            }

            try {
                replyTo.send(reply)
            } catch (_: Exception) {
                // Client is gone
            }
        }
    }

    // ── Image EXIF/XMP parsing (metadata-extractor) ───────────────────────

    @Suppress("DEPRECATION")
    private fun parseImageMetadata(input: Bundle): Bundle {
        val pfd = input.getParcelable<ParcelFileDescriptor>(KEY_PFD)
            ?: return Bundle().apply { putBoolean(KEY_ERROR, true) }
        val label = input.getString(KEY_LABEL, "")

        return pfd.use { fd ->
            // Primary: stream straight into metadata-extractor (the common JPEG/PNG/HEIF path).
            val meta = runCatching {
                FileInputStream(fd.fileDescriptor).use { ImageMetadataReader.readMetadata(it) }
            }.getOrNull()

            if (meta != null) {
                bundleFromImageMetadata(meta, label)
            } else {
                // metadata-extractor cannot read this container (e.g. JXL: "File format could not
                // be determined"). Recover embedded EXIF from the ISO-BMFF 'Exif' box and re-parse
                // the raw TIFF payload with the same reader so date/camera/GPS/ISO still surface.
                val exifMeta = extractEmbeddedExif(fd)
                if (exifMeta != null) {
                    bundleFromImageMetadata(exifMeta, label)
                } else {
                    // No EXIF recoverable — signal error so the client's in-process dimension
                    // fallback (native size probe) runs and the sheet still shows resolution/size.
                    Bundle().apply { putBoolean(KEY_ERROR, true) }
                }
            }
        }
    }

    /**
     * Reads a bounded prefix of the shared descriptor, walks the ISO-BMFF box tree for an embedded
     * `Exif` box (JXL BMFF; also a generic `Exif\u0000\u0000` preamble), and re-parses the extracted
     * raw TIFF/EXIF payload with metadata-extractor. Returns null if none is found. Rewinds the
     * descriptor first since the primary streaming read advanced its offset.
     */
    private fun extractEmbeddedExif(fd: ParcelFileDescriptor): com.drew.metadata.Metadata? =
        runCatching {
            Os.lseek(fd.fileDescriptor, 0, OsConstants.SEEK_SET)
            val bytes = readCappedBytes(fd, MAX_EXIF_SCAN_BYTES)
            val tiff = BmffExifExtractor.extractExifTiff(bytes) ?: return null
            ImageMetadataReader.readMetadata(ByteArrayInputStream(tiff))
        }.getOrNull()

    /** Reads up to [cap] bytes from [fd]'s current offset into a heap buffer. */
    private fun readCappedBytes(fd: ParcelFileDescriptor, cap: Int): ByteArray =
        FileInputStream(fd.fileDescriptor).use { stream ->
            val buffer = ByteArray(cap)
            var total = 0
            while (total < cap) {
                val read = stream.read(buffer, total, cap - total)
                if (read < 0) break
                total += read
            }
            if (total == cap) buffer else buffer.copyOf(total)
        }

    private fun bundleFromImageMetadata(
        meta: com.drew.metadata.Metadata,
        label: String
    ): Bundle {
        val result = Bundle()

        // EXIF IFD0
        meta.getDirectoriesOfType(ExifIFD0Directory::class.java).forEach { dir ->
            if (!result.containsKey(KEY_IMAGE_DESCRIPTION))
                dir.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION)
                    ?.let { result.putString(KEY_IMAGE_DESCRIPTION, it) }
            if (!result.containsKey(KEY_MANUFACTURER))
                dir.getString(ExifIFD0Directory.TAG_MAKE)
                    ?.let { result.putString(KEY_MANUFACTURER, it) }
            if (!result.containsKey(KEY_MODEL))
                dir.getString(ExifIFD0Directory.TAG_MODEL)
                    ?.let { result.putString(KEY_MODEL, it) }
            if (result.getInt(KEY_IMAGE_WIDTH, 0) == 0)
                dir.getInteger(ExifIFD0Directory.TAG_IMAGE_WIDTH)
                    ?.let { result.putInt(KEY_IMAGE_WIDTH, it) }
            if (result.getInt(KEY_IMAGE_HEIGHT, 0) == 0)
                dir.getInteger(ExifIFD0Directory.TAG_IMAGE_HEIGHT)
                    ?.let { result.putInt(KEY_IMAGE_HEIGHT, it) }
            if (!result.containsKey(KEY_RES_X))
                dir.getDoubleObject(ExifIFD0Directory.TAG_X_RESOLUTION)
                    ?.let { result.putDouble(KEY_RES_X, it) }
            if (!result.containsKey(KEY_RES_Y))
                dir.getDoubleObject(ExifIFD0Directory.TAG_Y_RESOLUTION)
                    ?.let { result.putDouble(KEY_RES_Y, it) }
            if (!result.containsKey(KEY_RES_UNIT))
                dir.getInteger(ExifIFD0Directory.TAG_RESOLUTION_UNIT)
                    ?.let { result.putInt(KEY_RES_UNIT, it) }
        }

        // EXIF SubIFD
        meta.getDirectoriesOfType(ExifSubIFDDirectory::class.java).forEach { dir ->
            if (!result.containsKey(KEY_DATETIME_ORIGINAL))
                dir.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
                    ?.let { result.putString(KEY_DATETIME_ORIGINAL, it) }
            if (!result.containsKey(KEY_APERTURE))
                dir.getDescription(ExifSubIFDDirectory.TAG_FNUMBER)
                    ?.let { result.putString(KEY_APERTURE, it) }
            if (!result.containsKey(KEY_EXPOSURE_TIME))
                dir.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)
                    ?.let { result.putString(KEY_EXPOSURE_TIME, it) }
            if (!result.containsKey(KEY_ISO))
                dir.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)
                    ?.let { result.putString(KEY_ISO, it) }
            if (result.getInt(KEY_IMAGE_WIDTH, 0) == 0)
                dir.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH)
                    ?.let { result.putInt(KEY_IMAGE_WIDTH, it) }
            if (result.getInt(KEY_IMAGE_HEIGHT, 0) == 0)
                dir.getInteger(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT)
                    ?.let { result.putInt(KEY_IMAGE_HEIGHT, it) }
        }

        // GPS
        meta.getDirectoriesOfType(GpsDirectory::class.java).forEach { dir ->
            dir.geoLocation?.let {
                if (!result.containsKey(KEY_GPS_LAT)) {
                    result.putDouble(KEY_GPS_LAT, it.latitude)
                    result.putDouble(KEY_GPS_LON, it.longitude)
                }
            }
        }

        // XMP + feature flags
        val xmps = meta.getDirectoriesOfType(XmpDirectory::class.java)

        // Night mode
        val isNightMode = meta.getDirectoriesOfType(ExifSubIFDDirectory::class.java).any { dir ->
            dir.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) &&
                    dir.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME) &&
                    run {
                        val isoVal = dir.getInt(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)
                        val expVal = dir.getDouble(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)
                        label.matches("(?i).*\\.NIGHT\\..*".toRegex()) ||
                                (isoVal < 100 && expVal > 0.01)
                    }
        }
        result.putBoolean(KEY_IS_NIGHT_MODE, isNightMode)

        // Photosphere
        val isPhotosphere = xmps.any { xmp ->
            xmp.xmpProperties.any { prop ->
                (PHOTOSPHERE_KEYS.any { key -> prop.key.contains(key, true) }
                        && prop.value == PHOTOSPHERE_VALUES[0])
                        || prop.value == PHOTOSPHERE_VALUES[1]
            }
        }
        result.putBoolean(KEY_IS_PHOTOSPHERE, isPhotosphere)

        // Panorama (but not photosphere)
        val isPanorama = xmps.any { xmp ->
            xmp.xmpProperties.any { prop ->
                PANORAMA_KEYS.any { key -> prop.key.contains(key, true) }
            }
        } && !isPhotosphere
        result.putBoolean(KEY_IS_PANORAMA, isPanorama)

        // Long exposure
        val isLongExposure = xmps.any { xmp ->
            xmp.xmpProperties.any { prop ->
                LONG_EXPOSURE_KEYS.any { key -> prop.key.contains(key, true) }
            }
        }
        result.putBoolean(KEY_IS_LONG_EXPOSURE, isLongExposure)

        // Motion photo
        val isMotionPhoto = xmps.any { xmp ->
            xmp.xmpProperties.any { prop ->
                (prop.key == "GCamera:MotionPhoto" && prop.value == "1") ||
                        (prop.key == "GCamera:MicroVideo" && prop.value == "1")
            }
        }
        result.putBoolean(KEY_IS_MOTION_PHOTO, isMotionPhoto)

        // Fallback dimensions via BitmapFactory not possible in isolated process
        // (no content resolver), so we return what we have. The client can fallback.

        return result
    }

    // ── Video metadata parsing (MediaMetadataRetriever) ───────────────────

    @Suppress("DEPRECATION")
    private fun parseVideoMetadata(input: Bundle): Bundle {
        val pfd = input.getParcelable<ParcelFileDescriptor>(KEY_PFD)
            ?: return Bundle().apply { putBoolean(KEY_ERROR, true) }

        return pfd.use { fd ->
            val result = Bundle()

            // ── 1) MediaMetadataRetriever (best-effort) ───────────────────
            // setDataSource throws a RuntimeException on some containers/codecs
            // (e.g. HEVC .MOV from iPhones). Treat that as non-fatal so the
            // summary — and the "View all metadata" entry point — still appears;
            // dimensions are recovered from metadata-extractor below.
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(fd.fileDescriptor)

                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.let { result.putLong(KEY_DURATION_MS, it) }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull()?.let { result.putInt(KEY_VIDEO_WIDTH, it) }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull()?.let { result.putInt(KEY_VIDEO_HEIGHT, it) }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toIntOrNull()?.let { result.putInt(KEY_BIT_RATE, it) }

                val frameRate = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    ?.toFloatOrNull()
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                        ?.let { cnt ->
                            result.getLong(KEY_DURATION_MS, 0L).takeIf { it > 0 }
                                ?.let { d -> cnt.toFloat() / (d / 1000f) }
                        }
                frameRate?.let { result.putFloat(KEY_FRAME_RATE, it) }
            } catch (_: Exception) {
                // Non-fatal: fall through to metadata-extractor.
            } finally {
                retriever.release()
            }

            // ── 2) metadata-extractor fallback for dimensions ─────────────
            // Recovers width/height from the QuickTime/MP4 video track when the
            // retriever failed or omitted them. Rewind the shared descriptor first.
            if (!result.containsKey(KEY_VIDEO_WIDTH) || !result.containsKey(KEY_VIDEO_HEIGHT)) {
                runCatching {
                    Os.lseek(fd.fileDescriptor, 0, OsConstants.SEEK_SET)
                    FileInputStream(fd.fileDescriptor).use { stream ->
                        val meta = ImageMetadataReader.readMetadata(stream)
                        meta.getFirstDirectoryOfType(QuickTimeVideoDirectory::class.java)?.let { dir ->
                            if (!result.containsKey(KEY_VIDEO_WIDTH))
                                dir.getInteger(QuickTimeVideoDirectory.TAG_WIDTH)
                                    ?.takeIf { it > 0 }?.let { result.putInt(KEY_VIDEO_WIDTH, it) }
                            if (!result.containsKey(KEY_VIDEO_HEIGHT))
                                dir.getInteger(QuickTimeVideoDirectory.TAG_HEIGHT)
                                    ?.takeIf { it > 0 }?.let { result.putInt(KEY_VIDEO_HEIGHT, it) }
                        }
                        meta.getFirstDirectoryOfType(Mp4VideoDirectory::class.java)?.let { dir ->
                            if (!result.containsKey(KEY_VIDEO_WIDTH))
                                dir.getInteger(Mp4VideoDirectory.TAG_WIDTH)
                                    ?.takeIf { it > 0 }?.let { result.putInt(KEY_VIDEO_WIDTH, it) }
                            if (!result.containsKey(KEY_VIDEO_HEIGHT))
                                dir.getInteger(Mp4VideoDirectory.TAG_HEIGHT)
                                    ?.takeIf { it > 0 }?.let { result.putInt(KEY_VIDEO_HEIGHT, it) }
                        }
                    }
                }
            }

            result
        }
    }

    // ── Raw metadata for the "View all metadata" screen ───────────────────

    @Suppress("DEPRECATION")
    private fun parseRawMetadata(input: Bundle): Bundle {
        val pfd = input.getParcelable<ParcelFileDescriptor>(KEY_PFD)
            ?: return Bundle().apply { putBoolean(KEY_ERROR, true) }
        val isVideo = input.getBoolean(KEY_IS_VIDEO, false)

        return pfd.use { fd ->
            if (isVideo) {
                parseRawVideoMetadata(fd)
            } else {
                parseRawImageMetadata(fd)
            }
        }
    }

    private fun parseRawImageMetadata(pfd: ParcelFileDescriptor): Bundle {
        val result = Bundle()

        // Primary streaming read. Falls back to the ISO-BMFF 'Exif' box (e.g. JXL) when
        // metadata-extractor can't read the container, so the "View all metadata" screen still
        // lists the embedded EXIF directories for those formats.
        val metadata = runCatching {
            FileInputStream(pfd.fileDescriptor).use { ImageMetadataReader.readMetadata(it) }
        }.getOrNull() ?: extractEmbeddedExif(pfd)

        val dirNames = ArrayList<String>()
        var dirIndex = 0
        metadata?.directories?.forEach { directory ->
            val tags = directory.tags.filter { it.description != null }
            if (tags.isEmpty()) return@forEach
            val dirKey = "dir_$dirIndex"
            dirNames.add(directory.name)
            val tagNames = ArrayList<String>(tags.size)
            val tagDescs = ArrayList<String>(tags.size)
            for (tag in tags) {
                tagNames.add(tag.tagName)
                tagDescs.add(tag.description)
            }
            result.putStringArrayList("${dirKey}_names", tagNames)
            result.putStringArrayList("${dirKey}_descs", tagDescs)
            dirIndex++
        }
        result.putStringArrayList(KEY_RAW_DIR_NAMES, dirNames)
        return result
    }

    @Suppress("DEPRECATION")
    private fun parseRawVideoMetadata(pfd: ParcelFileDescriptor): Bundle {
        val result = Bundle()
        val dirNames = ArrayList<String>()
        var dirIndex = 0

        // ── 1) MediaMetadataRetriever (container/stream level tags) ────────
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(pfd.fileDescriptor)
            val tagNames = ArrayList<String>()
            val tagDescs = ArrayList<String>()

            val keyMap = mapOf(
                MediaMetadataRetriever.METADATA_KEY_ALBUM to "Album",
                MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST to "Album Artist",
                MediaMetadataRetriever.METADATA_KEY_ARTIST to "Artist",
                MediaMetadataRetriever.METADATA_KEY_AUTHOR to "Author",
                MediaMetadataRetriever.METADATA_KEY_BITRATE to "Bitrate",
                MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER to "Track Number",
                MediaMetadataRetriever.METADATA_KEY_COMPILATION to "Compilation",
                MediaMetadataRetriever.METADATA_KEY_COMPOSER to "Composer",
                MediaMetadataRetriever.METADATA_KEY_DATE to "Date",
                MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER to "Disc Number",
                MediaMetadataRetriever.METADATA_KEY_DURATION to "Duration",
                MediaMetadataRetriever.METADATA_KEY_GENRE to "Genre",
                MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO to "Has Audio",
                MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO to "Has Video",
                MediaMetadataRetriever.METADATA_KEY_LOCATION to "Location",
                MediaMetadataRetriever.METADATA_KEY_MIMETYPE to "MIME Type",
                MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS to "Number of Tracks",
                MediaMetadataRetriever.METADATA_KEY_TITLE to "Title",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT to "Video Height",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH to "Video Width",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION to "Video Rotation",
                MediaMetadataRetriever.METADATA_KEY_WRITER to "Writer",
                MediaMetadataRetriever.METADATA_KEY_YEAR to "Year",
                MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT to "Frame Count",
                MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE to "Capture Frame Rate",
                MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD to "Color Standard",
                MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER to "Color Transfer",
                MediaMetadataRetriever.METADATA_KEY_COLOR_RANGE to "Color Range",
                MediaMetadataRetriever.METADATA_KEY_SAMPLERATE to "Sample Rate",
                MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE to "Bits Per Sample",
            )
            for ((key, name) in keyMap) {
                retriever.extractMetadata(key)?.let { value ->
                    tagNames.add(name)
                    tagDescs.add(value)
                }
            }
            if (tagNames.isNotEmpty()) {
                val dirKey = "dir_$dirIndex"
                dirNames.add("Video Metadata")
                result.putStringArrayList("${dirKey}_names", tagNames)
                result.putStringArrayList("${dirKey}_descs", tagDescs)
                dirIndex++
            }
        } catch (_: Exception) {
            // setDataSource throws on some containers/codecs (e.g. HEVC .MOV).
            // Non-fatal: metadata-extractor below still reads the QuickTime tags.
        } finally {
            retriever.release()
        }

        // ── 2) metadata-extractor (rich container EXIF: QuickTime/MOV, MP4) ─
        // MediaMetadataRetriever does not surface QuickTime/EXIF tags such as
        // camera Make/Model, lens or focal length. metadata-extractor reads the
        // QuickTime ('mvhd'/'mebx'/'mdta') and MP4 atoms that hold them, so .MOV
        // files from iPhones show their full metadata like images do. Rewind the
        // shared file descriptor first since the retriever advanced its offset.
        runCatching {
            Os.lseek(pfd.fileDescriptor, 0, OsConstants.SEEK_SET)
            FileInputStream(pfd.fileDescriptor).use { stream ->
                val metadata = ImageMetadataReader.readMetadata(stream)
                for (directory in metadata.directories) {
                    val tags = directory.tags.filter { it.description != null }
                    if (tags.isEmpty()) continue
                    val dirKey = "dir_$dirIndex"
                    dirNames.add(directory.name)
                    val tagNames = ArrayList<String>(tags.size)
                    val tagDescs = ArrayList<String>(tags.size)
                    for (tag in tags) {
                        tagNames.add(tag.tagName)
                        tagDescs.add(tag.description)
                    }
                    result.putStringArrayList("${dirKey}_names", tagNames)
                    result.putStringArrayList("${dirKey}_descs", tagDescs)
                    dirIndex++
                }
            }
        }

        result.putStringArrayList(KEY_RAW_DIR_NAMES, dirNames)
        return result
    }

    companion object {
        // Message types
        const val MSG_PARSE_IMAGE = 1
        const val MSG_PARSE_VIDEO = 2
        const val MSG_PARSE_RAW_METADATA = 3

        /**
         * Upper bound on the prefix read when recovering an embedded EXIF box from a container
         * metadata-extractor can't read (e.g. JXL). The EXIF box sits near the file start in
         * practice; capping the read keeps peak memory bounded in the isolated process.
         */
        private const val MAX_EXIF_SCAN_BYTES = 8 * 1024 * 1024

        // Bundle keys — input
        const val KEY_PFD = "pfd"
        const val KEY_LABEL = "label"
        const val KEY_IS_VIDEO = "is_video"

        // Bundle keys — output (image EXIF)
        const val KEY_IMAGE_DESCRIPTION = "image_description"
        const val KEY_DATETIME_ORIGINAL = "datetime_original"
        const val KEY_MANUFACTURER = "manufacturer"
        const val KEY_MODEL = "model"
        const val KEY_APERTURE = "aperture"
        const val KEY_EXPOSURE_TIME = "exposure_time"
        const val KEY_ISO = "iso"
        const val KEY_GPS_LAT = "gps_lat"
        const val KEY_GPS_LON = "gps_lon"
        const val KEY_IMAGE_WIDTH = "image_width"
        const val KEY_IMAGE_HEIGHT = "image_height"
        const val KEY_RES_X = "res_x"
        const val KEY_RES_Y = "res_y"
        const val KEY_RES_UNIT = "res_unit"

        // Bundle keys — output (video)
        const val KEY_DURATION_MS = "duration_ms"
        const val KEY_VIDEO_WIDTH = "video_width"
        const val KEY_VIDEO_HEIGHT = "video_height"
        const val KEY_FRAME_RATE = "frame_rate"
        const val KEY_BIT_RATE = "bit_rate"

        // Bundle keys — output (flags)
        const val KEY_IS_NIGHT_MODE = "is_night_mode"
        const val KEY_IS_PANORAMA = "is_panorama"
        const val KEY_IS_PHOTOSPHERE = "is_photosphere"
        const val KEY_IS_LONG_EXPOSURE = "is_long_exposure"
        const val KEY_IS_MOTION_PHOTO = "is_motion_photo"

        // Bundle keys — output (raw metadata)
        const val KEY_RAW_DIR_NAMES = "raw_dir_names"

        // Bundle keys — error
        const val KEY_ERROR = "error"
        const val KEY_ERROR_MESSAGE = "error_message"

        // Feature flag detection constants (mirrored from MediaMetadata companion)
        val PANORAMA_KEYS = arrayOf("ProjectionType", "FullPanoHeightPixels", "FullPanoWidthPixels")
        val PHOTOSPHERE_KEYS = arrayOf("IsPhotosphere", "UsePanoramaViewer")
        val PHOTOSPHERE_VALUES = arrayOf(
            "True",
            "com.google.android.apps.camera.gallery.specialtype.SpecialType-PHOTOSPHERE"
        )
        val LONG_EXPOSURE_KEYS = arrayOf("BurstID", "CameraBurstID")
    }
}

/**
 * Recovers a raw TIFF/EXIF payload from an ISO-BMFF container that metadata-extractor can't parse
 * directly (notably JPEG XL, whose EXIF lives in an `Exif` box). Runs entirely inside the isolated
 * process so a malformed file can't reach the main app.
 *
 * The returned bytes start at the TIFF header (`II*\u0000` / `MM\u0000*`) so they can be handed
 * straight back to [ImageMetadataReader.readMetadata], which auto-detects TIFF and populates the
 * standard Exif/GPS directories.
 */
private object BmffExifExtractor {

    private const val EXIF_BOX_TYPE = "Exif"

    /** Extracts the embedded EXIF/TIFF payload, or null if none is present in the scanned prefix. */
    fun extractExifTiff(bytes: ByteArray): ByteArray? {
        walkBoxes(bytes)?.let { return it }
        // Some writers store the JPEG-style "Exif\u0000\u0000" preamble followed by the TIFF header
        // (e.g. inside an mdat). Scan for it as a fallback.
        return findExifPreamble(bytes)
    }

    /** Iterates top-level ISO-BMFF boxes looking for an `Exif` box and returns its TIFF payload. */
    private fun walkBoxes(bytes: ByteArray): ByteArray? {
        val n = bytes.size
        var pos = 0
        while (pos + 8 <= n) {
            var boxSize = readU32(bytes, pos)
            val type = String(bytes, pos + 4, 4, Charsets.ISO_8859_1)
            var headerSize = 8
            when (boxSize) {
                1L -> {
                    if (pos + 16 > n) break
                    boxSize = readU64(bytes, pos + 8)
                    headerSize = 16
                }
                0L -> boxSize = (n - pos).toLong() // extends to end of file
            }
            if (boxSize < headerSize) break
            val payloadStart = pos + headerSize
            val payloadEnd = (pos + boxSize).coerceAtMost(n.toLong()).toInt()
            if (payloadStart > payloadEnd) break

            if (type == EXIF_BOX_TYPE) {
                parseExifBoxPayload(bytes, payloadStart, payloadEnd)?.let { return it }
            }

            val next = pos + boxSize
            if (next <= pos || next > Int.MAX_VALUE) break
            pos = next.toInt()
        }
        return null
    }

    /**
     * JXL `Exif` box payload begins with a 4-byte big-endian offset to the TIFF header (usually 0),
     * per ISO/IEC 18181-2. Returns the bytes from the TIFF header onward.
     */
    private fun parseExifBoxPayload(bytes: ByteArray, start: Int, end: Int): ByteArray? {
        if (end - start < 4) return null
        val tiffOffset = readU32(bytes, start).toInt()
        val tiffStart = start + 4 + tiffOffset
        if (tiffStart in start until end && isTiffHeader(bytes, tiffStart)) {
            return bytes.copyOfRange(tiffStart, end)
        }
        // Some writers omit the 4-byte offset and start the TIFF header immediately.
        if (isTiffHeader(bytes, start)) return bytes.copyOfRange(start, end)
        return null
    }

    /** Scans for a JPEG-style "Exif\u0000\u0000" preamble immediately followed by a TIFF header. */
    private fun findExifPreamble(bytes: ByteArray): ByteArray? {
        val n = bytes.size
        var i = 0
        while (i + 6 < n) {
            if (bytes[i] == 'E'.code.toByte() && bytes[i + 1] == 'x'.code.toByte() &&
                bytes[i + 2] == 'i'.code.toByte() && bytes[i + 3] == 'f'.code.toByte() &&
                bytes[i + 4] == 0x00.toByte() && bytes[i + 5] == 0x00.toByte()
            ) {
                val tiffStart = i + 6
                if (isTiffHeader(bytes, tiffStart)) return bytes.copyOfRange(tiffStart, n)
            }
            i++
        }
        return null
    }

    /** True if [off] points at a TIFF header: little-endian (II 2A 00) or big-endian (MM 00 2A). */
    private fun isTiffHeader(bytes: ByteArray, off: Int): Boolean {
        if (off < 0 || off + 4 > bytes.size) return false
        val le = bytes[off] == 0x49.toByte() && bytes[off + 1] == 0x49.toByte() &&
                bytes[off + 2] == 0x2A.toByte() && bytes[off + 3] == 0x00.toByte()
        val be = bytes[off] == 0x4D.toByte() && bytes[off + 1] == 0x4D.toByte() &&
                bytes[off + 2] == 0x00.toByte() && bytes[off + 3] == 0x2A.toByte()
        return le || be
    }

    private fun readU32(bytes: ByteArray, off: Int): Long =
        ((bytes[off].toLong() and 0xFF) shl 24) or
                ((bytes[off + 1].toLong() and 0xFF) shl 16) or
                ((bytes[off + 2].toLong() and 0xFF) shl 8) or
                (bytes[off + 3].toLong() and 0xFF)

    private fun readU64(bytes: ByteArray, off: Int): Long {
        var value = 0L
        for (k in 0 until 8) {
            value = (value shl 8) or (bytes[off + k].toLong() and 0xFF)
        }
        return value
    }
}
