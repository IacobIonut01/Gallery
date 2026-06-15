/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ocr_results",
    indices = [Index(value = ["fullText"])]
)
data class OcrResultEntity(
    @PrimaryKey
    val mediaId: Long,
    val fullText: String,
    val blocksJson: String = "[]",
    val timestamp: Long = 0L
)
