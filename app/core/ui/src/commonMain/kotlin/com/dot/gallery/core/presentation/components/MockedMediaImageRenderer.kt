/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import kotlin.math.abs

/**
 * A [MediaImageRenderer] that renders colored placeholder boxes instead of real images.
 * Uses a fixed palette, cycling based on [model]'s hashCode.
 */
val MockedMediaImageRenderer = object : MediaImageRenderer {
    @Composable
    override fun RenderImage(
        modifier: Modifier,
        model: Any?,
        contentScale: ContentScale,
        contentDescription: String?,
        signature: Any?
    ) {
        val colors = listOf(
            Color(0xFFBBDEFB), // light blue
            Color(0xFFC8E6C9), // light green
            Color(0xFFFFCDD2), // light red
            Color(0xFFFFF9C4), // light yellow
            Color(0xFFD1C4E9), // light purple
        )
        val color = colors[(model?.hashCode()?.let { abs(it) } ?: 0) % colors.size]
        Box(modifier = modifier.background(color))
    }
}
