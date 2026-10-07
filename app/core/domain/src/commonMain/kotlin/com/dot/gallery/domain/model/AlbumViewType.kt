/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
enum class AlbumViewType : Parcelable {
    GRID, LIST
}
