/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.isVideo
import com.dot.gallery.feature_node.presentation.mediaview.components.video.VideoPlayer

@Stable
@NonRestartableComposable
@Composable
fun MediaPreviewComponent(
    media: Media,
    uiEnabled: Boolean,
    playWhenReady: Boolean,
    onItemClick: () -> Unit,
    onSwipeDown: () -> Unit,
    videoController: @Composable (ExoPlayer, MutableState<Boolean>, MutableLongState, Long, Int, Float) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = media.isVideo,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VideoPlayer(
                media = media,
                playWhenReady = playWhenReady,
                videoController = videoController,
                onItemClick = onItemClick,
                onSwipeDown = onSwipeDown
            )
        }

        AnimatedVisibility(
            visible = !media.isVideo,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ZoomablePagerImage(
                media = media,
                uiEnabled = uiEnabled,
                onItemClick = onItemClick,
                onSwipeDown = onSwipeDown
            )
        }
    }
}