/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components.media

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dot.gallery.core.presentation.components.util.tapAndGesture
import com.dot.gallery.feature_node.domain.model.Media
import kotlin.math.abs
import kotlin.math.withSign

@Composable
fun ZoomablePagerImage(
    modifier: Modifier = Modifier,
    media: Media,
    scrollEnabled: MutableState<Boolean>,
    minScale: Float = 1f,
    maxScale: Float = 10f,
    maxImageSize: Int,
    isRotation: Boolean = false,
    onItemClick: () -> Unit
) {
    var targetScale by remember { mutableStateOf(1f) }
    val scale = animateFloatAsState(
        targetValue = maxOf(minScale, minOf(maxScale, targetScale)),
        label = "Image Scale"
    )
    var rotationState by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(1f) }
    var offsetY by remember { mutableStateOf(1f) }
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(media.uri)
            .memoryCacheKey("media_${media.label}_${media.id}")
            .diskCacheKey("media_${media.label}_${media.id}")
            .size(maxImageSize)
            .build(),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None
    )

    val onDoubleTap: (Offset) -> Unit = remember {
        {
            if (targetScale >= 2f) {
                targetScale = 1f
                offsetX = 1f
                offsetY = 1f
                scrollEnabled.value = true
            } else targetScale = 3f
        }
    }

    val onGesture: ((centeroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit) = remember {
        { _, pan, zoom, rotation ->
            targetScale *= zoom
            if (targetScale <= 1) {
                offsetX = 1f
                offsetY = 1f
                targetScale = 1f
                scrollEnabled.value = true
            } else {
                offsetX += pan.x
                offsetY += pan.y
                if (zoom > 1) {
                    scrollEnabled.value = false
                    rotationState += rotation
                }
                val imageWidth = screenWidthPx * scale.value
                val borderReached = imageWidth - screenWidthPx - 2 * abs(offsetX)
                scrollEnabled.value = borderReached <= 0
                if (borderReached < 0) {
                    offsetX = ((imageWidth - screenWidthPx) / 2f).withSign(offsetX)
                    if (pan.x != 0f) offsetY -= pan.y
                }
            }
        }
    }

    Image(
        modifier = modifier
            .fillMaxSize()
            .tapAndGesture(
                onTap = { onItemClick() },
                onDoubleTap = onDoubleTap,
                onGesture = onGesture,
                scrollEnabled = scrollEnabled
            )
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                if (isRotation) {
                    rotationZ = rotationState
                }
                translationX = offsetX
                translationY = offsetY
            },
        painter = painter,
        contentScale = ContentScale.Fit,
        contentDescription = media.label
    )
}