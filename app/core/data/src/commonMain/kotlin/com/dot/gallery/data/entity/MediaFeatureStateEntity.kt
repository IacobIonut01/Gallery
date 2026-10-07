/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

@Entity(
    tableName = "media_feature_state",
    primaryKeys = ["mediaId", "feature"],
    indices = [
        Index(value = ["feature", "status"]),
        Index(value = ["status"]),
        Index(value = ["leaseExpiresAt"]),
        Index(value = ["runId"])
    ]
)
data class MediaFeatureStateEntity(
    val mediaId: Long,
    val feature: MediaFeature,
    @ColumnInfo(defaultValue = "'pending'")
    val status: MediaFeatureStatus = MediaFeatureStatus.PENDING,
    @ColumnInfo(defaultValue = "''")
    val sourceRevision: String = "",
    @ColumnInfo(defaultValue = "''")
    val resultRevision: String = "",
    @ColumnInfo(defaultValue = "0")
    val attemptCount: Int = 0,
    val updatedAt: Long,
    val lastAttemptAt: Long? = null,
    val nextRetryAt: Long? = null,
    val leaseOwner: String? = null,
    val leaseExpiresAt: Long? = null,
    val runId: String? = null,
    val lastErrorCode: String? = null
)
