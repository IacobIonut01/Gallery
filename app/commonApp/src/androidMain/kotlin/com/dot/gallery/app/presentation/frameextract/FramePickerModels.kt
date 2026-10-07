package com.dot.gallery.app.presentation.frameextract

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import com.dot.gallery.domain.model.Media
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.domain.model.Vault
import com.dot.gallery.domain.util.getUri
import kotlinx.parcelize.Parcelize
import java.io.File
import com.dot.gallery.domain.util.MotionPhotoInfo
import com.dot.gallery.domain.util.isCloud
import com.dot.gallery.domain.util.isEncrypted
import com.dot.gallery.domain.uri.scheme


@Parcelize
data class FrameSourceSpec(
    val mediaId: Long,
    val label: String,
    val uri: String,
    val mimeType: String,
    val sourceKind: FrameSourceKind,
    val relativePath: String,
    val captureTimestampMs: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val motionPhotoHint: Boolean = false,
    val preferredPresentationTimeUs: Long = -1L,
    val initialPositionMs: Long? = null,
    val sourceSize: Long = 0L,
    val vaultId: String? = null,
) : Parcelable {
    val isMotionPhotoCandidate: Boolean
        get() = motionPhotoHint || mimeType.startsWith("image/")

    companion object {
        fun from(
            media: Media,
            metadata: MediaMetadata? = null,
            currentVault: Vault? = null,
            motionPhotoHint: Boolean = false,
            preferredPresentationTimeUs: Long = -1L,
            initialPositionMs: Long? = null,
        ): FrameSourceSpec {
            val sourceKind = when {
                media.isCloud -> FrameSourceKind.CLOUD
                media.isEncrypted -> FrameSourceKind.VAULT
                media.getUri().scheme == "content" &&
                    media.getUri().authority != "media" -> FrameSourceKind.DOCUMENT
                else -> FrameSourceKind.LOCAL
            }
            return FrameSourceSpec(
                mediaId = media.id,
                label = media.label,
                uri = media.getUri().toString(),
                mimeType = media.mimeType,
                sourceKind = sourceKind,
                relativePath = media.relativePath,
                captureTimestampMs = media.takenTimestamp ?: media.timestamp * 1000L,
                latitude = metadata?.gpsLatitude,
                longitude = metadata?.gpsLongitude,
                motionPhotoHint = motionPhotoHint,
                preferredPresentationTimeUs = preferredPresentationTimeUs,
                initialPositionMs = initialPositionMs,
                sourceSize = media.size,
                vaultId = currentVault?.uuid?.toString(),
            )
        }
    }
}

enum class FrameSourceOwnership {
    DIRECT,
    SESSION
}

data class PreparedFrameSource(
    val sourceUri: Uri,
    val localFile: File?,
    val ownership: FrameSourceOwnership,
    val ownershipToken: String,
    val source: FrameSourceSpec,
    val motionPhotoInfo: MotionPhotoInfo? = null,
) {
    val isMotionPhoto: Boolean get() = motionPhotoInfo != null

    fun deleteIfOwned() {
        if (ownership == FrameSourceOwnership.SESSION) {
            localFile?.let {
                FrameSourceCleanup.release(it)
                it.delete()
            }
        }
    }
}

enum class FrameExportFormat(
    val persistedValue: String,
    val extension: String,
    val mimeType: String,
    val quality: Int,
    val compressFormat: Bitmap.CompressFormat,
) {
    JPEG("jpeg", "jpg", "image/jpeg", 95, Bitmap.CompressFormat.JPEG),
    PNG("png", "png", "image/png", 100, Bitmap.CompressFormat.PNG);

    companion object {
        fun fromPersisted(value: String?): FrameExportFormat =
            entries.firstOrNull { it.persistedValue == value } ?: JPEG
    }
}

data class FramePreview(
    val identity: FrameIdentity,
    val bitmap: Bitmap,
)

sealed interface FramePickerUiState {
    data class PreparingSource(val progress: Int?) : FramePickerUiState
    data class Ready(
        val metadata: FrameVideoMetadata,
        val currentFrame: FramePreview?,
        val selection: List<FrameIdentity>,
        val selectedThumbnails: List<FramePreview>,
        val filmstrip: List<FramePreview>,
        val isPreviewLoading: Boolean,
        val isPlaying: Boolean,
        val format: FrameExportFormat,
        val preferredTimeUs: Long,
    ) : FramePickerUiState
    data class Exporting(val done: Int, val total: Int, val phase: String) : FramePickerUiState
    data class PartialSuccess(val saved: List<Uri>, val failed: Int, val warnings: Int) : FramePickerUiState
    data class Success(val savedUris: List<Uri>, val warnings: Int) : FramePickerUiState
    data class Failure(val reason: String, val retryable: Boolean) : FramePickerUiState
}
