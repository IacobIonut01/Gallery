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
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SyncState

data class CloudMediaLocalState(
    val remoteId: String,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val localCopyPath: String,
    val size: Long,
    val timestamp: Long,
    val syncState: SyncState,
    val appLocalCopy: Boolean = false
)
