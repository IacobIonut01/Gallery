/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "media_metadata_flags")
data class MediaMetadataFlags(
    @PrimaryKey val mediaId: Long,
    val isNightMode: Boolean,
    val isPanorama: Boolean,
    val isPhotosphere: Boolean,
    val isLongExposure: Boolean,
    val isMotionPhoto: Boolean
)
