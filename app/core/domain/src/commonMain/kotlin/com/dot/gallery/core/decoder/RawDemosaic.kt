/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import kotlin.math.pow

/** Demosaic (interpolation) algorithm, mapped to LibRaw's `user_qual`. */
enum class RawDemosaic(val nativeValue: Int) {
    LINEAR(0), VNG(1), PPG(2), AHD(3), DCB(4)
}