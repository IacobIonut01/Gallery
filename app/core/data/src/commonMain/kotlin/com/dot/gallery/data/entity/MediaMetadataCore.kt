/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "media_metadata_core")
data class MediaMetadataCore(
    @PrimaryKey val mediaId: Long,
    val imageDescription: String?,
    val dateTimeOriginal: String?,
    val manufacturerName: String?,
    val modelName: String?,
    @ColumnInfo(defaultValue = "")
    val lensModel: String? = null,
    val aperture: String?,
    val exposureTime: String?,
    val iso: String?,
    @ColumnInfo(defaultValue = "0.0")
    val focalLength: Double? = null,
    val gpsLatitude: Double?,
    val gpsLongitude: Double?,
    val gpsLocationName: String?,
    val gpsLocationNameCountry: String?,
    val gpsLocationNameCity: String?,
    val imageWidth: Int,
    val imageHeight: Int,
    val imageResolutionX: Double?,
    val imageResolutionY: Double?,
    val resolutionUnit: Int?
)
