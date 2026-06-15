/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_sections")
@Immutable
data class AlbumSection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(defaultValue = "")
    val label: String = "",

    @ColumnInfo(defaultValue = "2")
    val type: Int = AlbumSectionType.CUSTOM.value,

    @ColumnInfo(defaultValue = "0")
    val displayOrder: Int = 0,

    @ColumnInfo(defaultValue = "1")
    val isVisible: Boolean = true,

    @ColumnInfo(defaultValue = "1")
    val isExpanded: Boolean = true,

    @ColumnInfo(defaultValue = "")
    val sortKind: String = "",

    @ColumnInfo(defaultValue = "")
    val sortOrder: String = "",
) {
    val sectionType: AlbumSectionType
        get() = AlbumSectionType.fromValue(type)
}

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
