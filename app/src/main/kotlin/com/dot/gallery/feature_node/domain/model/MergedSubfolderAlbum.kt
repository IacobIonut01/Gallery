/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merged_subfolder_table",
    indices = [Index(value = ["folderKey"], unique = true)]
)
@Immutable
data class MergedSubfolderAlbum(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    @ColumnInfo(defaultValue = "NULL")
    val folderKey: String? = null,
    @ColumnInfo(defaultValue = "''")
    val volume: String = "",
    @ColumnInfo(defaultValue = "''")
    val relativePath: String = "",
    @ColumnInfo(defaultValue = "'combined'")
    val displayMode: String = DISPLAY_MODE_COMBINED
) {
    companion object {
        const val DISPLAY_MODE_COMBINED = "combined"
        const val DISPLAY_MODE_SUB_GALLERY = "sub_gallery"

        fun folderKey(volume: String, relativePath: String): String =
            "${volume.trimEnd('/')}/${relativePath.trim('/')}"
    }
}
