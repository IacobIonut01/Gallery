/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert

data class PendingMetadataLocation(
    val mediaId: Long,
    val gpsLatitude: Double,
    val gpsLongitude: Double,
)
