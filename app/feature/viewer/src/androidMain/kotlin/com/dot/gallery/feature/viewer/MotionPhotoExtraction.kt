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


data class MotionPhotoExtraction(
    val info: MotionPhotoInfo? = null,
    val videoFile: File? = null,
    val durationMs: Long = 0L,
    val compositeFilmstrip: Bitmap? = null
)