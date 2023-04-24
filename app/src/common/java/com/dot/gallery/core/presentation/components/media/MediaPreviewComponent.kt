/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components.media

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.RequestBuilder
import com.dot.gallery.feature_node.domain.model.Media

@Composable
fun MediaPreviewComponent(
    media: Media,
    scrollEnabled: MutableState<Boolean>,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    playWhenReady: Boolean,
    onItemClick: () -> Unit,
    videoController: @Composable (ExoPlayer, MutableState<Long>, Long, Int, () -> Unit) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color.Black,
            ),
    ) {
        if (media.duration != null) {
            VideoPlayer(
                media = media,
                playWhenReady = playWhenReady,
                videoController = videoController,
                onItemClick = onItemClick
            )
        } else {
            ZoomablePagerImage(
                modifier = Modifier.fillMaxSize(),
                media = media,
                scrollEnabled = scrollEnabled,
                preloadRequestBuilder = preloadRequestBuilder,
                onItemClick = onItemClick
            )
        }
    }
}