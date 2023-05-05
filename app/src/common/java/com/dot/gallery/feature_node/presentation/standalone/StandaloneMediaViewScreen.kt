/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.standalone

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.DEFAULT_LOW_VELOCITY_SWIPE_DURATION
import com.dot.gallery.core.Constants.HEADER_DATE_FORMAT
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Settings.Glide.rememberMaxImageSize
import com.dot.gallery.core.presentation.components.media.MediaPreviewComponent
import com.dot.gallery.core.presentation.components.media.VideoPlayerController
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewAppBar
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewBottomBar
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.toggleSystemBars

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StandaloneMediaViewScreen(
    paddingValues: PaddingValues,
    mediaState: MutableState<MediaState>,
    handler: MediaHandleUseCase
) {
    val state by mediaState
    val pagerState = rememberPagerState()
    val scrollEnabled = remember { mutableStateOf(true) }

    val currentDate = remember { mutableStateOf("") }

    val showUI = remember { mutableStateOf(true) }
    val context = LocalContext.current
    val maxImageSize by rememberMaxImageSize()
    val window = with(LocalContext.current as Activity) { return@with window }
    val windowInsetsController =
        remember { WindowCompat.getInsetsController(window, window.decorView) }

    /** Glide Preloading **/
    val preloadingData = rememberGlidePreloadingData(
        data = state.media,
        preloadImageSize = Size(512f, 384f)
    ) { media: Media, requestBuilder: RequestBuilder<Drawable> ->
        requestBuilder
            .signature(MediaStoreSignature(media.mimeType, media.timestamp, media.orientation))
            .load(media.uri)
    }
    /** ************ **/

    LaunchedEffect(state.media) {
        state.media.firstOrNull()?.let {
            currentDate.value = it.timestamp.getDate(HEADER_DATE_FORMAT)
        }
    }
    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            pageCount = state.media.size,
            userScrollEnabled = scrollEnabled.value,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                lowVelocityAnimationSpec = tween(
                    easing = LinearEasing,
                    durationMillis = DEFAULT_LOW_VELOCITY_SWIPE_DURATION
                )
            ),
            pageSpacing = 16.dp,
            modifier = Modifier
                .background(Color.Black)
                .fillMaxSize()
        ) { index ->
            val (media, preloadRequestBuilder) = preloadingData[index]
            MediaPreviewComponent(
                media = media,
                scrollEnabled = scrollEnabled,
                maxImageSize = maxImageSize,
                preloadRequestBuilder = preloadRequestBuilder,
                playWhenReady = index == pagerState.currentPage,
                onItemClick = {
                    showUI.value = !showUI.value
                    windowInsetsController.toggleSystemBars(showUI.value)
                }
            ) { player: ExoPlayer, currentTime: MutableState<Long>, totalTime: Long, buffer: Int, playToggle: () -> Unit ->
                AnimatedVisibility(
                    visible = showUI.value,
                    enter = enterAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
                    exit = exitAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
                    modifier = Modifier.fillMaxSize()
                ) {
                    VideoPlayerController(
                        paddingValues = paddingValues,
                        player = player,
                        currentTime = currentTime,
                        totalTime = totalTime,
                        buffer = buffer,
                        playToggle = playToggle
                    )
                }
            }
        }
        MediaViewAppBar(
            showUI = showUI.value,
            currentDate = currentDate.value,
            paddingValues = paddingValues,
            onGoBack = (context as Activity)::finish
        )
        MediaViewBottomBar(
            showDeleteButton = false,
            handler = handler,
            showUI = showUI.value,
            paddingValues = paddingValues,
            currentMedia = state.media.firstOrNull()
        )
    }

}