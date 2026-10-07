/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "media_metadata_video")
data class MediaMetadataVideo(
    @PrimaryKey val mediaId: Long,
    val durationMs: Long?,
    val videoWidth: Int?,
    val videoHeight: Int?,
    val frameRate: Float?,
    val bitRate: Int?
)
