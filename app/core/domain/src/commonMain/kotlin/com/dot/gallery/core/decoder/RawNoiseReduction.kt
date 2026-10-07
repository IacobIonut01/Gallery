/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import kotlin.math.pow

/** Noise-reduction strength, mapped to LibRaw's FBDD + wavelet threshold. */
enum class RawNoiseReduction(val fbdd: Int, val threshold: Float) {
    OFF(0, 0f), LOW(1, 100f), MEDIUM(2, 300f), HIGH(2, 700f)
}