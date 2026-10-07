/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import kotlin.math.pow

/** Highlight-recovery mode, mapped to LibRaw's `highlight`. */
enum class RawHighlightMode(val nativeValue: Int) {
    CLIP(0), UNCLIP(1), BLEND(2), REBUILD(5)
}