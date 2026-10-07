/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "smart_scan_phases",
    primaryKeys = ["runId", "phase"],
    foreignKeys = [
        ForeignKey(
            entity = SmartScanRunEntity::class,
            parentColumns = ["runId"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["runId"]),
        Index(value = ["status"]),
        Index(value = ["leaseExpiresAt"])
    ]
)
data class SmartScanPhaseEntity(
    val runId: String,
    val phase: SmartScanPhase,
    @ColumnInfo(defaultValue = "'queued'")
    val status: SmartScanStatus = SmartScanStatus.QUEUED,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val updatedAt: Long,
    val leaseOwner: String? = null,
    val leaseExpiresAt: Long? = null,
    @ColumnInfo(defaultValue = "''")
    val processorRevision: String = "",
    @ColumnInfo(defaultValue = "0")
    val attemptCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val totalMedia: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val processedMedia: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val succeededMedia: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val skippedMedia: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val failedMedia: Int = 0,
    val lastErrorCode: String? = null
)
