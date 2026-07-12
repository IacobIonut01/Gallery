/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Visual transition used when the slideshow advances from one item to the next.
 *
 * - [SLIDE]: the default horizontal pager slide.
 * - [FADE]: cross-fade between adjacent pages.
 * - [KEN_BURNS]: cross-fade combined with a slow zoom/pan on images.
 */
@Serializable
@Parcelize
enum class SlideshowTransition : Parcelable {
    SLIDE, FADE, KEN_BURNS
}
