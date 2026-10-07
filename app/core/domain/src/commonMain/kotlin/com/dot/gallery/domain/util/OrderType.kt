/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.util

import com.dot.gallery.domain.parcel.Parcelable
import com.dot.gallery.domain.parcel.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
sealed class OrderType : Parcelable {
    // SerialName preserves the pre-KMP FQN stored in TimelineSettings JSON
    // column defaults/rows (polymorphic "type" discriminator).
    @Serializable
    @Parcelize
    @SerialName("com.dot.gallery.feature_node.domain.util.OrderType.Ascending")
    data object Ascending : OrderType()

    @Serializable
    @Parcelize
    @SerialName("com.dot.gallery.feature_node.domain.util.OrderType.Descending")
    data object Descending : OrderType()
}
