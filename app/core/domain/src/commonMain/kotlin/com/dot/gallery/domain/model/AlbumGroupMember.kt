/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Immutable
import androidx.room3.Entity
import androidx.room3.ForeignKey

@Entity(
    tableName = "album_group_members",
    primaryKeys = ["groupId", "albumId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Immutable
data class AlbumGroupMember(
    val groupId: Long,
    val albumId: Long
)
