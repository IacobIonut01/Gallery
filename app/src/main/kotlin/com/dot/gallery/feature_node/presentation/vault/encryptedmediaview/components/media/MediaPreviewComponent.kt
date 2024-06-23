/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.vault.encryptedmediaview.components.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import com.dot.gallery.feature_node.domain.model.EncryptedMedia
import com.dot.gallery.feature_node.presentation.vault.encryptedmediaview.components.video.VideoPlayer

@Composable
fun MediaPreviewComponent(
    media: EncryptedMedia,
    uiEnabled: Boolean,
    playWhenReady: Boolean,
    onItemClick: () -> Unit,
    videoController: @Composable (ExoPlayer, MutableState<Boolean>, MutableState<Long>, Long, Int, Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (media.isVideo) {
            VideoPlayer(
                media = media,
                playWhenReady = playWhenReady,
                videoController = videoController,
                onItemClick = onItemClick
            )
        } else {
            ZoomablePagerImage(
                media = media,
                uiEnabled = uiEnabled,
                onItemClick = onItemClick
            )
        }
    }
}