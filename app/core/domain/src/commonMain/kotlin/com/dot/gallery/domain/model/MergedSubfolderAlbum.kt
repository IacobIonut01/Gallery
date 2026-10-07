/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

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

        /**
         * Virtual albums synthesised by the subfolder-merge resolver get ids at or
         * above this base so they can never collide with real MediaStore album ids.
         */
        const val VIRTUAL_ALBUM_ID_BASE = 1L shl 32

        fun isVirtualAlbumId(id: Long): Boolean = id >= VIRTUAL_ALBUM_ID_BASE

        fun folderKey(volume: String, relativePath: String): String =
            "${volume.trimEnd('/')}/${relativePath.trim('/')}"
    }
}
