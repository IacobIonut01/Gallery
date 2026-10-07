/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import com.dot.gallery.domain.uri.path

data class CloudMediaSmartFeatureRevision(
    val remoteId: String,
    val path: String,
    val label: String,
    val mimeType: String,
    val timestamp: Long,
    val size: Long
)

/** Result of [CloudMediaDao.getSyncStateStats] — item count + total byte size. */
