/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.mediaview.components.video.VideoPlayer

@Composable
fun MediaPreviewComponent(
    media: Media,
    scrollEnabled: MutableState<Boolean>,
    maxImageSize: Int,
    playWhenReady: Boolean,
    onItemClick: () -> Unit,
    videoController: @Composable (ExoPlayer, MutableState<Long>, Long, Int, () -> Unit) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (media.mimeType.contains("video")) {
            VideoPlayer(
                media = media,
                playWhenReady = playWhenReady,
                videoController = videoController,
                onItemClick = onItemClick
            )
        } else {
            ZoomablePagerImage(
                media = media,
                scrollEnabled = scrollEnabled,
                maxImageSize = maxImageSize,
                onItemClick = onItemClick
            )
        }
    }
}