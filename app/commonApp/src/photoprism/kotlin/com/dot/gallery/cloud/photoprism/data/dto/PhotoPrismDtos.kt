/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism.data.dto

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.data.entity.CloudMediaEntity
import com.google.gson.annotations.SerializedName

data class PhotoPrismSessionDto(
    @SerializedName("access_token") val accessToken: String? = null,
    /** Deprecated alias for access_token on older PhotoPrism builds. */
    val id: String? = null,
    val status: String? = null,
    val code: Int? = null,
    val error: String? = null,
    @SerializedName("messageId") val messageId: String? = null,
    val config: PhotoPrismConfigDto? = null,
    val user: PhotoPrismUserDto? = null
) {
    fun resolvedAccessToken(): String? =
        accessToken?.takeIf { it.isNotBlank() } ?: id?.takeIf { it.isNotBlank() }

    fun requiresPasscode(): Boolean = code == 32
}

data class PhotoPrismUserDto(
    @SerializedName("UID") val uid: String? = null,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Email") val email: String? = null,
    @SerializedName("Admin") val admin: Boolean = false
)

data class PhotoPrismConfigDto(
    val name: String? = null,
    val version: String? = null,
    val previewToken: String? = null,
    val downloadToken: String? = null
)

data class PhotoPrismOAuthTokenDto(
    @SerializedName("access_token") val accessToken: String = "",
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null
)

data class PhotoPrismAlbumDto(
    @SerializedName("UID") val uid: String = "",
    @SerializedName("Title") val title: String = "",
    @SerializedName("PhotoCount") val photoCount: Int = 0,
    @SerializedName("LinkCount") val linkCount: Int = 0,
    @SerializedName("CreatedAt") val createdAt: String? = null,
    @SerializedName("UpdatedAt") val updatedAt: String? = null
)

/**
 * Named subject from GET /api/v1/subjects (people when Type == "person").
 * Thumb is a file hash (optionally with crop area) used with /api/v1/t/{Thumb}/…
 */
data class PhotoPrismSubjectDto(
    @SerializedName("UID") val uid: String = "",
    @SerializedName("Type") val type: String = "person",
    @SerializedName("Slug") val slug: String = "",
    @SerializedName("Name") val name: String = "",
    @SerializedName("Alias") val alias: String = "",
    @SerializedName("Favorite") val favorite: Boolean = false,
    @SerializedName("Hidden") val hidden: Boolean = false,
    @SerializedName("Private") val private: Boolean = false,
    @SerializedName("Excluded") val excluded: Boolean = false,
    @SerializedName("FileCount") val fileCount: Int = 0,
    @SerializedName("PhotoCount") val photoCount: Int = 0,
    @SerializedName("Thumb") val thumb: String = "",
    @SerializedName("ThumbSrc") val thumbSrc: String? = null
)

/** GeoJSON FeatureCollection from `GET /api/v1/geo`. */
data class PhotoPrismGeoCollectionDto(
    val type: String? = null,
    val features: List<PhotoPrismGeoFeatureDto> = emptyList()
)

data class PhotoPrismGeoFeatureDto(
    val type: String? = null,
    val geometry: PhotoPrismGeoGeometryDto? = null,
    val properties: PhotoPrismGeoPropertiesDto? = null
)

data class PhotoPrismGeoGeometryDto(
    val type: String? = null,
    /** GeoJSON Point: `[longitude, latitude]`. */
    val coordinates: List<Double>? = null
)

data class PhotoPrismGeoPropertiesDto(
    @SerializedName("UID") val uid: String = "",
    @SerializedName("Hash") val hash: String? = null,
    @SerializedName("Title") val title: String? = null,
    @SerializedName("TakenAt") val takenAt: String? = null
)

data class PhotoPrismFileDto(
    @SerializedName("UID") val uid: String = "",
    @SerializedName("Hash") val hash: String = "",
    @SerializedName("Name") val name: String = "",
    @SerializedName("Size") val size: Long = 0L,
    @SerializedName("Primary") val primary: Boolean = false,
    @SerializedName("Mime") val mime: String? = null,
    @SerializedName("Width") val width: Int = 0,
    @SerializedName("Height") val height: Int = 0,
    @SerializedName("MediaType") val mediaType: String? = null,
    @SerializedName("Duration") val duration: Long? = null
)

data class PhotoPrismPhotoDto(
    @SerializedName("UID") val uid: String = "",
    @SerializedName("Type") val type: String = "image",
    @SerializedName("Title") val title: String = "",
    @SerializedName("Description") val description: String? = null,
    @SerializedName("FileName") val fileName: String = "",
    @SerializedName("FileUID") val fileUid: String = "",
    @SerializedName("Hash") val hash: String = "",
    @SerializedName("Width") val width: Int = 0,
    @SerializedName("Height") val height: Int = 0,
    @SerializedName("TakenAt") val takenAt: String? = null,
    @SerializedName("TakenAtLocal") val takenAtLocal: String? = null,
    @SerializedName("CreatedAt") val createdAt: String? = null,
    @SerializedName("UpdatedAt") val updatedAt: String? = null,
    @SerializedName("Favorite") val favorite: Boolean = false,
    @SerializedName("Private") val private: Boolean = false,
    @SerializedName("Path") val path: String = "",
    @SerializedName("Name") val name: String = "",
    @SerializedName("OriginalName") val originalName: String = "",
    @SerializedName("CameraMake") val cameraMake: String? = null,
    @SerializedName("CameraModel") val cameraModel: String? = null,
    @SerializedName("LensModel") val lensModel: String? = null,
    @SerializedName("Iso") val iso: Int? = null,
    @SerializedName("FocalLength") val focalLength: Int? = null,
    @SerializedName("FNumber") val fNumber: Double? = null,
    @SerializedName("Exposure") val exposure: String? = null,
    @SerializedName("Lat") val latitude: Double? = null,
    @SerializedName("Lng") val longitude: Double? = null,
    @SerializedName("PlaceCity") val placeCity: String? = null,
    @SerializedName("PlaceState") val placeState: String? = null,
    @SerializedName("PlaceCountry") val placeCountry: String? = null,
    @SerializedName("Files") val files: List<PhotoPrismFileDto>? = null
) {
    fun primaryFile(): PhotoPrismFileDto? =
        files?.firstOrNull { it.primary } ?: files?.firstOrNull()

    fun resolvedHash(): String =
        hash.ifBlank { primaryFile()?.hash.orEmpty() }

    fun toCloudMediaEntity(
        serverConfigId: Long,
        baseUrl: String,
        previewToken: String?,
        downloadToken: String?
    ): CloudMediaEntity {
        val primary = primaryFile()
        val fileHash = resolvedHash()
        val mime = primary?.mime
            ?: when {
                type.equals("video", ignoreCase = true) ||
                    type.equals("live", ignoreCase = true) -> "video/mp4"
                type.equals("raw", ignoreCase = true) -> "image/x-adobe-dng"
                else -> "image/jpeg"
            }
        val label = originalName.ifBlank {
            fileName.substringAfterLast('/').ifBlank {
                name.ifBlank { title.ifBlank { uid } }
            }
        }
        val timestamp = parseIsoTimestamp(takenAt ?: takenAtLocal ?: createdAt ?: updatedAt ?: "")
        val durationMs = primary?.duration
        val durationStr = durationMs?.takeIf { it > 0 }?.let { ms ->
            val totalSec = ms / 1_000_000L // PhotoPrism duration is often nanoseconds for video
            if (totalSec > 86_400) {
                // Fallback: treat as milliseconds if value looks like ms
                val sec = ms / 1000L
                "%d:%02d".format(sec / 60, sec % 60)
            } else if (totalSec > 0) {
                "%d:%02d".format(totalSec / 60, totalSec % 60)
            } else null
        }
        val thumbSize = "fit_720"
        val thumbnailUrl = if (fileHash.isNotBlank() && !previewToken.isNullOrBlank()) {
            "$baseUrl/api/v1/t/$fileHash/$previewToken/$thumbSize"
        } else ""
        val originalUrl = if (fileHash.isNotBlank() && !downloadToken.isNullOrBlank()) {
            "$baseUrl/api/v1/dl/$fileHash?t=$downloadToken"
        } else ""

        return CloudMediaEntity(
            remoteId = uid,
            providerType = ProviderType.PHOTOPRISM,
            serverConfigId = serverConfigId,
            label = label,
            path = fileName.ifBlank { path },
            relativePath = path,
            mimeType = mime,
            timestamp = timestamp,
            takenTimestamp = parseIsoTimestamp(takenAtLocal ?: takenAt ?: "").takeIf { it > 0 },
            size = primary?.size ?: 0L,
            width = if (width > 0) width else primary?.width ?: 0,
            height = if (height > 0) height else primary?.height ?: 0,
            duration = durationStr,
            favorite = favorite,
            trashed = false,
            archived = private,
            syncState = SyncState.REMOTE_ONLY,
            contentHash = fileHash.ifBlank { null },
            thumbnailUrl = thumbnailUrl,
            originalUrl = originalUrl,
            latitude = latitude?.takeIf { it != 0.0 },
            longitude = longitude?.takeIf { it != 0.0 },
            city = placeCity,
            state = placeState,
            country = placeCountry,
            cameraMake = cameraMake,
            cameraModel = cameraModel,
            lensModel = lensModel,
            imageDescription = description,
            dateTimeOriginal = takenAtLocal ?: takenAt,
            exposureTime = exposure,
            aperture = fNumber?.let { "f/$it" },
            iso = iso,
            focalLength = focalLength?.toDouble(),
            fileId = fileHash
        )
    }

    companion object {
        fun parseIsoTimestamp(iso: String): Long {
            if (iso.isBlank()) return 0L
            return try {
                java.time.Instant.parse(iso).toEpochMilli()
            } catch (_: Exception) {
                try {
                    java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli()
                } catch (_: Exception) {
                    try {
                        java.time.LocalDateTime.parse(iso.replace(" ", "T"))
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        0L
                    }
                }
            }
        }
    }
}
