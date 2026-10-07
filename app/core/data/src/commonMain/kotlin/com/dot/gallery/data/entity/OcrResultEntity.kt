/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey


@Entity(
    tableName = "ocr_results",
    indices = [Index(value = ["fullText"])]
)
data class OcrResultEntity(
    @PrimaryKey
    val mediaId: Long,
    val fullText: String,
    val blocksJson: String = "[]",
    val timestamp: Long = 0L,
    @ColumnInfo(defaultValue = "''")
    val resultRevision: String = ""
)
