/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.dot.gallery.domain.model.CaptureTimeOrigin
import com.dot.gallery.domain.model.ResolvedCaptureTime
import com.dot.gallery.domain.model.UriMedia

@Entity(
    tableName = "media_capture_time",
    indices = [Index(value = ["origin"])]
)
data class MediaCaptureTimeEntity(
    @PrimaryKey val mediaId: Long,
    val captureTimestampMillis: Long,
    val origin: CaptureTimeOrigin,
    val sourceModifiedSeconds: Long,
    val sourceTakenTimestampMillis: Long?,
    val sourceSize: Long,
    val sourcePath: String,
    val updatedAtMillis: Long
) {
    fun isFreshFor(media: UriMedia): Boolean =
        sourceModifiedSeconds == media.timestamp &&
            sourceTakenTimestampMillis == media.takenTimestamp &&
            sourceSize == media.size &&
            sourcePath == media.path
}

fun MediaCaptureTimeEntity.applyTo(media: UriMedia): UriMedia {
    if (!isFreshFor(media)) return media
    val takenTimestamp = captureTimestampMillis.takeUnless {
        origin == CaptureTimeOrigin.MODIFIED_FALLBACK
    }
    return media.copy(
        takenTimestamp = takenTimestamp,
        captureTimeOrigin = origin
    )
}

fun ResolvedCaptureTime.toEntity(
    media: UriMedia,
    updatedAtMillis: Long
): MediaCaptureTimeEntity = MediaCaptureTimeEntity(
    mediaId = media.id,
    captureTimestampMillis = timestampMillis,
    origin = origin,
    sourceModifiedSeconds = media.timestamp,
    sourceTakenTimestampMillis = media.takenTimestamp,
    sourceSize = media.size,
    sourcePath = media.path,
    updatedAtMillis = updatedAtMillis
)
