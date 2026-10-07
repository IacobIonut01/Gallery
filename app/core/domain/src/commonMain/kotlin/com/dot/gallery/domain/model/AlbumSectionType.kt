/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

enum class AlbumSectionType(val value: Int) {
    COMMON(0),
    APPS(1),
    CUSTOM(2),
    UNCATEGORIZED(3);

    companion object {
        fun fromValue(value: Int): AlbumSectionType =
            entries.firstOrNull { it.value == value } ?: CUSTOM
    }
}
