/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.dot.gallery.BuildConfig

object Constants {

    const val AUTHORITY: String = BuildConfig.CONTENT_AUTHORITY

    /**
     * Default logging tag
     */
    const val TAG = "DotGallery"

    /**
     * Date format used in media groups
     */
    const val WEEKLY_DATE_FORMAT = "EEEE"
    const val DEFAULT_DATE_FORMAT = "EEE, MMMM d"
    const val EXTENDED_DATE_FORMAT = "EEE, MMM d, yyyy"
    const val FULL_DATE_FORMAT = "EEEE, MMMM d, yyyy, hh:mm a"
    const val HEADER_DATE_FORMAT = "MMMM d, yyyy\n" + "h:mm a"
    const val EXIF_DATE_FORMAT = "MMMM d, yyyy • h:mm a"

    /**
     * Value in ms
     */
    const val DEFAULT_LOW_VELOCITY_SWIPE_DURATION = 150

    /**
     * Smooth enough at 300ms
     */
    const val DEFAULT_NAVIGATION_ANIMATION_DURATION = 300

    /**
     * Syncs with status bar fade in/out
     */
    const val DEFAULT_TOP_BAR_ANIMATION_DURATION = 1000

    /**
     * MAX Image Size in Media Preview
     * Android LIMIT: 4096x4096 [16MP]
     */
    const val MAX_IMAGE_SIZE = 4096

    private val PERMISSION_COMMON = listOf(
        Manifest.permission.ACCESS_MEDIA_LOCATION
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val PERMISSION_T = PERMISSION_COMMON.toMutableList().apply {
        addAll(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        )
    }

    private val PERMISSION_OLD =
        PERMISSION_COMMON.toMutableList().apply {
            addAll(
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }

    val PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PERMISSION_T
    } else {
        PERMISSION_OLD
    }


    /**
     * Animations
     */
    object Animation {

        val enterAnimation = fadeIn(tween(DEFAULT_LOW_VELOCITY_SWIPE_DURATION))
        val exitAnimation = fadeOut(tween(DEFAULT_LOW_VELOCITY_SWIPE_DURATION))

        val navigateInAnimation = fadeIn(tween(DEFAULT_NAVIGATION_ANIMATION_DURATION))
        val navigateUpAnimation = fadeOut(tween(DEFAULT_NAVIGATION_ANIMATION_DURATION))

        fun enterAnimation(durationMillis: Int): EnterTransition =
            fadeIn(tween(durationMillis))

        fun exitAnimation(durationMillis: Int): ExitTransition =
            fadeOut(tween(durationMillis))

    }

    object Target {
        const val TARGET_FAVORITES = "favorites"
        const val TARGET_TRASH = "trash"
    }
}