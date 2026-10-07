/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.Entity
import androidx.room3.ForeignKey

@Entity(
    tableName = "album_section_members",
    primaryKeys = ["sectionId", "albumId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumSection::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Immutable
data class AlbumSectionMember(
    val sectionId: Long,
    val albumId: Long
)
