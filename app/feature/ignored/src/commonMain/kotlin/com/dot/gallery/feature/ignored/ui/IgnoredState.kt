/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui

import androidx.compose.runtime.Immutable
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize

@Immutable
@Parcelize
data class IgnoredState(
    val albums: List<IgnoredAlbum> = emptyList()
) : Parcelable
