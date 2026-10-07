/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import kotlin.math.pow

/** Output colour space, mapped to LibRaw's `output_color`. */
enum class RawOutputColorSpace(val nativeValue: Int) {
    RAW(0), SRGB(1), ADOBE_RGB(2), WIDE_GAMUT(3), PROPHOTO(4)
}