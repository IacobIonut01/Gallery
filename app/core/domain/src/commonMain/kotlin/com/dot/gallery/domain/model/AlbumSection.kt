/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
)

val AlbumSection.sectionType: AlbumSectionType
    get() = AlbumSectionType.fromValue(type)
