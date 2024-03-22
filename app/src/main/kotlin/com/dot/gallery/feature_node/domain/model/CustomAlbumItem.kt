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




@Entity(tableName = "customalbum_items", primaryKeys = ["id","albumId"])
@Immutable
@Parcelize
data class CustomAlbumItem(
    val id: Long = 0,
    val albumId: Long = 0
) : Parcelable