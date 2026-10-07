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

enum class FaceLinkKind(val storedValue: String) {
    INCLUDE("include"),
    EXCLUDE("exclude");

    companion object {
        fun fromStored(value: String): FaceLinkKind =
            entries.first { it.storedValue == value }
    }
}
