package com.dot.gallery.feature_node.presentation.frameextract

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object ExtractedFrameDestination {
    const val FALLBACK = "Pictures/ReFra/Extracted Frames"

    fun resolve(sourceKind: FrameSourceKind, sourceRelativePath: String): String {
        if (sourceKind != FrameSourceKind.LOCAL) return FALLBACK
        val normalized = sourceRelativePath.replace('\\', '/').trim().trimStart('/')
        val root = normalized.substringBefore('/').lowercase(Locale.ROOT)
        return if ((root == "dcim" || root == "pictures") &&
            normalized.split('/').none { it == "." || it == ".." }
        ) normalized.trimEnd('/') else FALLBACK
    }
}

object ExtractedFrameName {
    fun create(
        sourceLabel: String,
        identity: FrameIdentity,
        format: FrameExportFormat,
        collisionToken: String = UUID.randomUUID().toString().take(6),
    ): String {
        val base = sanitize(sourceLabel.substringBeforeLast('.', sourceLabel)).ifBlank { "frame" }
        val timestamp = formatTimestamp(identity.presentationTimeUs)
        val index = identity.frameIndex.takeIf { it >= 0 }?.toString() ?: "time"
        return "${base}_frame_${timestamp}_${index}_${sanitize(collisionToken)}.${format.extension}"
    }

    fun sanitize(value: String): String = value
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '.')
        .take(80)

    private fun formatTimestamp(timeUs: Long): String {
        val totalMs = (timeUs.coerceAtLeast(0L) / 1000L)
        val hours = totalMs / 3_600_000L
        val minutes = (totalMs / 60_000L) % 60L
        val seconds = (totalMs / 1000L) % 60L
        val millis = totalMs % 1000L
        return "${hours.toString().padStart(2, '0')}-${minutes.toString().padStart(2, '0')}-${seconds.toString().padStart(2, '0')}-${millis.toString().padStart(3, '0')}"
    }
}

data class ExtractedFrameMetadata(
    val captureTimestampMs: Long,
    val latitude: Double?,
    val longitude: Double?,
)

data class ExtractedFrameWriteResult(
    val uri: Uri,
    val metadataWarning: Boolean,
)

class ExtractedFrameWriter(
    private val resolver: ContentResolver,
) {
    fun write(
        bitmap: Bitmap,
        source: FrameSourceSpec,
        identity: FrameIdentity,
        format: FrameExportFormat,
    ): ExtractedFrameWriteResult {
        val destination = ExtractedFrameDestination.resolve(source.sourceKind, source.relativePath)
        val displayName = ExtractedFrameName.create(source.label, identity, format)
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.DATE_TAKEN, source.captureTimestampMs)
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            put(MediaStore.Images.Media.ORIENTATION, 0)
            if (source.latitude != null && source.longitude != null) {
                put(MediaStore.Images.ImageColumns.LATITUDE, source.latitude)
                put(MediaStore.Images.ImageColumns.LONGITUDE, source.longitude)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, destination)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Unable to create MediaStore row")
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                if (!bitmap.compress(format.compressFormat, format.quality, output)) {
                    throw IOException("Frame encoding failed")
                }
            } ?: throw IOException("Unable to open output")
            val metadataWarning = !writeMetadata(
                uri = uri,
                metadata = ExtractedFrameMetadata(
                    captureTimestampMs = source.captureTimestampMs,
                    latitude = source.latitude,
                    longitude = source.longitude,
                ),
                width = bitmap.width,
                height = bitmap.height,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null,
                    ) <= 0
                ) {
                    throw IOException("Unable to publish extracted frame")
                }
            }
            return ExtractedFrameWriteResult(uri, metadataWarning)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun writeMetadata(
        uri: Uri,
        metadata: ExtractedFrameMetadata,
        width: Int,
        height: Int,
    ): Boolean = runCatching {
        resolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
            val exif = ExifInterface(descriptor.fileDescriptor)
            val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            exif.setAttribute(ExifInterface.TAG_DATETIME, formatter.format(Date(metadata.captureTimestampMs)))
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, formatter.format(Date(metadata.captureTimestampMs)))
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
            exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
            if (metadata.latitude != null && metadata.longitude != null) {
                exif.setGpsInfo(Location("frame-source").apply {
                    latitude = metadata.latitude
                    longitude = metadata.longitude
                    time = metadata.captureTimestampMs
                })
            }
            exif.saveAttributes()
        } ?: throw IOException("Unable to open metadata descriptor")
    }.isSuccess
}
