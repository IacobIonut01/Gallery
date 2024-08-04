/*
 * Copyright 2023 Dora Lee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sanghun.compose.video

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import androidx.media3.ui.PlayerView
import io.sanghun.compose.video.controller.VideoPlayerControllerConfig

@SuppressLint("UnsafeOptInUsageError")
internal fun VideoPlayerControllerConfig.applyToExoPlayerView(
    playerView: PlayerView,
    onFullScreenStatusChanged: (Boolean) -> Unit,
) {
    val controllerView = playerView.rootView

    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_settings).isVisible =
        showSpeedAndPitchOverlay
    playerView.setShowSubtitleButton(showSubtitleButton)
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_time).isVisible =
        showCurrentTimeAndTotalTime
    playerView.setShowBuffering(
        if (!showBufferingProgress) PlayerView.SHOW_BUFFERING_NEVER else PlayerView.SHOW_BUFFERING_ALWAYS,
    )
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_ffwd_with_amount).isVisible =
        showForwardIncrementButton
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_rew_with_amount).isVisible =
        showBackwardIncrementButton
    playerView.setShowNextButton(showNextTrackButton)
    playerView.setShowPreviousButton(showBackTrackButton)
    playerView.setShowFastForwardButton(showForwardIncrementButton)
    playerView.setShowRewindButton(showBackwardIncrementButton)
    playerView.controllerShowTimeoutMs = controllerShowTimeMilliSeconds
    playerView.controllerAutoShow = controllerAutoShow

    @Suppress("DEPRECATION")
    if (showFullScreenButton) {
        playerView.setControllerOnFullScreenModeChangedListener {
            onFullScreenStatusChanged(it)
        }
    } else {
        playerView.setControllerOnFullScreenModeChangedListener(null)
    }
}