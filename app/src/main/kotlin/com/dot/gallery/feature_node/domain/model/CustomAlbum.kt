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
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize


@Entity(tableName = "customalbum")
@Immutable
@Parcelize
data class CustomAlbum(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val timestamp: Long,
    val isPinned: Boolean = false
) : Parcelable {

    @IgnoredOnParcel
    @Ignore var count: Long = 0

    companion object {

        val NewAlbum = CustomAlbum(
            id = -200,
            label = "New Album",
            timestamp = 0
        )
    }
}