/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif

import com.dot.gallery.core.metadata.MetadataDirectory
import com.dot.gallery.core.metadata.MetadataTag
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.feature.albums.ui.util.formatSize

/**
 * Builds "View all metadata" directories from Room-backed cloud data.
 *
 * `cloud://` URIs cannot be opened via ContentResolver, so the isolated
 * metadata-extractor path always returns empty. PhotoPrism/Immich already sync
 * EXIF into [CloudMediaEntity] (and [MediaMetadata] via the metadata collection
 * worker), so we surface those fields instead.
 */
fun buildCloudMetadataDirectories(
    entity: CloudMediaEntity?,
    metadata: MediaMetadata?,
): List<MetadataDirectory> {
    if (entity == null && metadata == null) return emptyList()

    val directories = mutableListOf<MetadataDirectory>()
    var currentTags = mutableListOf<MetadataTag>()

    fun tag(name: String, value: String?) {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return
        currentTags += MetadataTag(name, trimmed)
    }

    fun flush(name: String) {
        if (currentTags.isNotEmpty()) {
            directories += MetadataDirectory(name, currentTags.toList())
            currentTags = mutableListOf()
        }
    }

    val label = entity?.label?.takeIf { it.isNotBlank() }
    val path = entity?.path?.takeIf { it.isNotBlank() }
    val mimeType = entity?.mimeType?.takeIf { it.isNotBlank() }
    val size = entity?.size?.takeIf { it > 0 }
    val provider = entity?.providerType?.displayName

    tag("Provider", provider)
    tag("File Name", label)
    tag("Path", path)
    tag("MIME Type", mimeType)
    size?.let { tag("File Size", formatSize(it)) }
    flush("File")

    val width = firstPositive(entity?.width, metadata?.imageWidth, metadata?.videoWidth)
    val height = firstPositive(entity?.height, metadata?.imageHeight, metadata?.videoHeight)
    val isVideo = entity?.mimeType?.startsWith("video") == true ||
        (metadata?.durationMs != null && (metadata.durationMs ?: 0) > 0) ||
        metadata?.videoWidth != null

    if (width != null && height != null) {
        tag(if (isVideo) "Video Width" else "Image Width", "$width pixels")
        tag(if (isVideo) "Video Height" else "Image Height", "$height pixels")
    }
    val duration = entity?.duration?.takeIf { it.isNotBlank() }
        ?: metadata?.durationMs?.takeIf { it > 0 }?.let { formatDurationMs(it) }
    tag("Duration", duration)
    metadata?.frameRate?.takeIf { it > 0 }?.let {
        tag("Frame Rate", formatFrameRate(it))
    }
    metadata?.bitRate?.takeIf { it > 0 }?.let { tag("Bit Rate", "$it bps") }
    flush(if (isVideo) "Video" else "Image")

    tag("Description", firstNonBlank(entity?.imageDescription, metadata?.imageDescription))
    tag("Date/Time Original", firstNonBlank(entity?.dateTimeOriginal, metadata?.dateTimeOriginal))
    tag("Make", firstNonBlank(entity?.cameraMake, metadata?.manufacturerName))
    tag("Model", firstNonBlank(entity?.cameraModel, metadata?.modelName))
    tag("Lens Model", firstNonBlank(entity?.lensModel, metadata?.lensModel))
    tag("F-Number", firstNonBlank(entity?.aperture, metadata?.aperture))
    tag("Exposure Time", firstNonBlank(entity?.exposureTime, metadata?.exposureTime))
    tag(
        "ISO",
        entity?.iso?.toString() ?: metadata?.iso?.takeIf { it.isNotBlank() }
    )
    val focal = entity?.focalLength?.takeIf { it > 0 }
        ?: metadata?.focalLength?.takeIf { it > 0 }
    focal?.let { fl ->
        tag(
            "Focal Length",
            if (fl == fl.toLong().toDouble()) "${fl.toLong()} mm" else "$fl mm"
        )
    }
    flush("Exif")

    val lat = entity?.latitude ?: metadata?.gpsLatitude
    val lon = entity?.longitude ?: metadata?.gpsLongitude
    if (lat != null && lon != null) {
        tag("Latitude", formatCoordinate(lat))
        tag("Longitude", formatCoordinate(lon))
    }
    tag("City", firstNonBlank(entity?.city, metadata?.gpsLocationNameCity))
    tag("State", entity?.state)
    tag("Country", firstNonBlank(entity?.country, metadata?.gpsLocationNameCountry))
    tag("Location", metadata?.gpsLocationName)
    flush("GPS")

    return directories
}

private fun firstNonBlank(vararg values: String?): String? =
    values.firstOrNull { !it.isNullOrBlank() }

private fun firstPositive(vararg values: Int?): Int? =
    values.firstOrNull { it != null && it > 0 }

private fun formatDurationMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}

private fun formatFrameRate(frameRate: Float): String {
    val scaled = (frameRate * 100f).toInt()
    val whole = scaled / 100
    val frac = (scaled % 100).let { if (it < 0) -it else it }
    return "$whole.${frac.toString().padStart(2, '0')} fps"
}

private fun formatCoordinate(value: Double): String {
    val scaled = (value * 1_000_000.0).toLong()
    val sign = if (scaled < 0) "-" else ""
    val abs = if (scaled < 0) -scaled else scaled
    val whole = abs / 1_000_000
    val frac = (abs % 1_000_000).toString().padStart(6, '0')
    return "$sign$whole.$frac"
}
