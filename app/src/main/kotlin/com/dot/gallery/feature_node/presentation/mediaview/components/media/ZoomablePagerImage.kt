/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dot.gallery.core.util.zoomable.customZoomable
import com.dot.gallery.core.util.zoomable.rememberZoomState
import com.dot.gallery.feature_node.domain.model.Media

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

    Image(
        modifier = modifier
            .fillMaxSize()
            .customZoomable(
                zoomState = zoomState,
                scrollEnabled = scrollEnabled,
                onClick = onItemClick
            ),
        painter = painter,
        contentScale = ContentScale.Fit,
        contentDescription = media.label
    )
}