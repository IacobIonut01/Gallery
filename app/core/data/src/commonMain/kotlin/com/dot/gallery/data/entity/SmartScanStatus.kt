/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
