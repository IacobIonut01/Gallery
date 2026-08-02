/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.data_source

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SmartScanTrigger(val storedValue: String) {
    AUTOMATIC("automatic"),
    MANUAL("manual"),
    RECOVERY("recovery")
}

enum class SmartScanStatus(val storedValue: String) {
    QUEUED("queued"),
    RUNNING("running"),
    SUCCEEDED("succeeded"),
    PARTIAL("partial"),
    BLOCKED("blocked"),
    INTERRUPTED("interrupted"),
    FAILED("failed"),
    CANCELLED("cancelled")
}

enum class SmartScanFeature(val bit: Int) {
    METADATA(1),
    EMBEDDINGS(1 shl 1),
    CATEGORIES(1 shl 2),
    PERSONS(1 shl 3);

    companion object {
        val ALL_MASK = entries.fold(0) { mask, feature -> mask or feature.bit }

        fun expandDependencies(mask: Int): Int =
            if (mask and CATEGORIES.bit != 0) mask or EMBEDDINGS.bit else mask
    }
}

enum class SmartScanPhase(val storedValue: String) {
    SOURCE_SYNC("source_sync"),
    METADATA("metadata"),
    SEARCH_INDEX("search_index"),
    CATEGORY_CLASSIFICATION("category_classification"),
    FACE_INDEX("face_index")
}

enum class MediaFeature(val storedValue: String) {
    METADATA("metadata"),
    SEARCH_EMBEDDING("search_embedding"),
    FACE_DETECTION("face_detection"),
    CATEGORY_CLASSIFICATION("category_classification")
}

enum class MediaFeatureStatus(val storedValue: String) {
    PENDING("pending"),
    PROCESSING("processing"),
    SUCCEEDED("succeeded"),
    SKIPPED("skipped"),
    BLOCKED("blocked"),
    FAILED("failed")
}

@Entity(
    tableName = "smart_scan_runs",
    indices = [
        Index(value = ["status"]),
        Index(value = ["requestedAt"]),
        Index(value = ["leaseExpiresAt"])
    ]
)
data class SmartScanRunEntity(
    @PrimaryKey
    val runId: String,
    val trigger: SmartScanTrigger,
    @ColumnInfo(defaultValue = "0")
    val requestedFeatures: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val userVisible: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val fullRefresh: Boolean = false,
    val workId: String? = null,
    val currentPhase: SmartScanPhase? = null,
    @ColumnInfo(defaultValue = "'queued'")
    val status: SmartScanStatus = SmartScanStatus.QUEUED,
    val requestedAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val updatedAt: Long = requestedAt,
    val leaseOwner: String? = null,
    val leaseExpiresAt: Long? = null,
    @ColumnInfo(defaultValue = "''")
    val sourceSnapshot: String = "",
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
