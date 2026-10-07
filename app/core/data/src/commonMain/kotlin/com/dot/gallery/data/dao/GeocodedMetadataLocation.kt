/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.data.dao
import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
data class GeocodedMetadataLocation(
    val mediaId: Long,
    val latitude: Double,
    val longitude: Double,
    val locationName: String?,
    val country: String?,
    val city: String?,
)
