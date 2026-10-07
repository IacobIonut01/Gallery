/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import com.dot.gallery.core.presentation.components.OptionPosition.ALONE
import com.dot.gallery.core.presentation.components.OptionPosition.BOTTOM
import com.dot.gallery.core.presentation.components.OptionPosition.MIDDLE
import com.dot.gallery.core.presentation.components.OptionPosition.TOP


enum class OptionPosition {
    TOP, MIDDLE, BOTTOM, ALONE
}

fun OptionPosition.shape(): RoundedCornerShape = when (this) {
    TOP -> OptionShape.Top
    MIDDLE -> OptionShape.Middle
    BOTTOM -> OptionShape.Bottom
    ALONE -> OptionShape.Alone
}
