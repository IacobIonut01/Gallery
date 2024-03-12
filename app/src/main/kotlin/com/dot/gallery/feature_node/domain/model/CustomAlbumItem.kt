/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize




@Entity(tableName = "customalbum_items")
@Immutable
@Parcelize
data class CustomAlbumItem(
    @PrimaryKey(autoGenerate = false)
    val id: Long = 0,
    val albumId: Long = 0
) : Parcelable {

    companion object {

        val CustomAlbumItem = CustomAlbumItem(
            id = -200,
            albumId = -200
        )
    }
}