/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import com.dot.gallery.core.presentation.components.FilterKind
import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize
import com.dot.gallery.domain.util.OrderType
import kotlinx.serialization.Serializable

/** Persisted sort selection — JSON-encoded into `*_sort_obj` prefs. */
@Serializable
@Parcelize
data class LastSort(
    val orderType: OrderType,
    val kind: FilterKind
) : Parcelable
