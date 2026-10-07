/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

import android.graphics.Bitmap
import androidx.media3.exoplayer.ExoPlayer
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.MotionPhotoInfo
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Motion-photo playback surface owned by the media viewer's ViewModel. The viewer-side
 * composables (filmstrip, surface, scrubber) see only this narrow contract so the
 * ViewModel itself can stay in the app module.
 */
interface MotionPhotoController {
    val motionPhotoExtraction: StateFlow<MotionPhotoExtraction>
    val motionPlayback: StateFlow<MotionPlaybackState>
    val motionPlayer: ExoPlayer?

    fun prepareMotionPhoto(media: Media?)
    fun toggleMotionPlayback()
    fun startMotionPlayback()
    fun stopMotionPlayback()
    fun seekMotionTo(ms: Long)
    fun seekMotionAndPause(ms: Long)
}
