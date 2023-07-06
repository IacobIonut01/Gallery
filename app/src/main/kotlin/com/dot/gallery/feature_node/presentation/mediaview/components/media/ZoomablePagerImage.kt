/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dot.gallery.feature_node.domain.model.Media
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun ZoomablePagerImage(
    modifier: Modifier = Modifier,
    media: Media,
    scrollEnabled: MutableState<Boolean>,
    maxScale: Float = 25f,
    maxImageSize: Int,
    onItemClick: () -> Unit
) {
    val zoomState = rememberZoomState(
        maxScale = maxScale
    )
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(media.uri)
            .memoryCacheKey("media_${media.label}_${media.id}")
            .diskCacheKey("media_${media.label}_${media.id}")
            .size(maxImageSize)
            .build(),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        onSuccess = {
            zoomState.setContentSize(it.painter.intrinsicSize)
        }
    )

    LaunchedEffect(zoomState.scale) {
        scrollEnabled.value = zoomState.scale == 1f
    }

    Image(
        modifier = modifier
            .fillMaxSize()
            .zoomable(
                zoomState = zoomState,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onItemClick
            ),
        painter = painter,
        contentScale = ContentScale.Fit,
        contentDescription = media.label
    )
}
