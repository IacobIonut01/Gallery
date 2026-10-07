/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ColorPaletteOption(
    val name: String,
    val seedArgb: Int,
    val hexKey: String,
)

val presetPalettes = listOf(
    ColorPaletteOption("Blue", 0xFF1673E1.toInt(), "FF1673E1"),
    ColorPaletteOption("Red", 0xFFC62828.toInt(), "FFC62828"),
    ColorPaletteOption("Green", 0xFF2E7D32.toInt(), "FF2E7D32"),
    ColorPaletteOption("Purple", 0xFF7B1FA2.toInt(), "FF7B1FA2"),
    ColorPaletteOption("Orange", 0xFFE65100.toInt(), "FFE65100"),
    ColorPaletteOption("Teal", 0xFF00897B.toInt(), "FF00897B"),
    ColorPaletteOption("Pink", 0xFFD81B60.toInt(), "FFD81B60"),
    ColorPaletteOption("Indigo", 0xFF283593.toInt(), "FF283593"),
    ColorPaletteOption("Amber", 0xFFFFA000.toInt(), "FFFFA000"),
    ColorPaletteOption("Cyan", 0xFF00ACC1.toInt(), "FF00ACC1"),
    ColorPaletteOption("Brown", 0xFF5D4037.toInt(), "FF5D4037"),
    ColorPaletteOption("Lime", 0xFF9E9D24.toInt(), "FF9E9D24"),
)
