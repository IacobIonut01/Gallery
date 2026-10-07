/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.ui.components.dashedBorder
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.library.generated.resources.Res
import com.dot.gallery.feature.library.generated.resources.categorise_your_media


/**
 * Empty-state for the categories carousel — dashed gradient box inviting the
 * user to categorise their media.
 *
 * The gradient icon uses an offscreen compositing layer + SrcIn instead of
 * `canvas.nativeCanvas.saveLayer` so the same effect renders on every
 * supported target.
 */
@Composable
fun NoCategories(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
        )
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(
                brush = brush,
                shape = RoundedCornerShape(16.dp),
                gapLength = 8.dp
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberVectorPainter(image = Icons.Outlined.ImageSearch),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = brush, blendMode = BlendMode.SrcIn)
                }
        )
        Text(
            text = stringResource(Res.string.categorise_your_media),
            style = MaterialTheme.typography.titleMedium.copy(brush = brush),
        )
    }
}
