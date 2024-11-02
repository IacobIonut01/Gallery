/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.video

import android.app.Activity
import android.media.MediaMetadataRetriever
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Settings.Misc.rememberAudioFocus
import com.dot.gallery.core.presentation.components.util.swipe
import com.dot.gallery.feature_node.domain.model.Media
import io.sanghun.compose.video.RepeatMode
import io.sanghun.compose.video.uri.VideoPlayerMediaItem
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import io.sanghun.compose.video.VideoPlayer as SanghunComposeVideoVideoPlayer

@Stable
@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@NonRestartableComposable
@Composable
fun VideoPlayer(
    media: Media,
    playWhenReady: Boolean,
    videoController: @Composable (ExoPlayer, MutableState<Boolean>, MutableLongState, Long, Int, Float) -> Unit,
    onItemClick: () -> Unit,
    onSwipeDown: () -> Unit
) {
    var totalDuration by rememberSaveable { mutableLongStateOf(0L) }
    val currentTime = rememberSaveable { mutableLongStateOf(0L) }
    var bufferedPercentage by rememberSaveable { mutableIntStateOf(0) }
    val isPlaying = rememberSaveable(playWhenReady) { mutableStateOf(playWhenReady) }
    var lastPlayingState by rememberSaveable(isPlaying.value) { mutableStateOf(isPlaying.value) }
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val metadata = remember(media) {
        MediaMetadataRetriever().apply {
            setDataSource(context, media.uri)
        }
    }
    val frameRate = remember(metadata) {
        metadata.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
        )?.toFloat() ?: 60f
    }

    var exoPlayer by remember {
        mutableStateOf<ExoPlayer?>(null)
    }

    var showPlayer by remember {
        mutableStateOf(false)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                showPlayer = true
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                showPlayer = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(showPlayer) {
        if (showPlayer) {
            delay(100)
            exoPlayer?.playWhenReady = isPlaying.value
            exoPlayer?.seekTo(currentTime.longValue)
        }
    }

    if (showPlayer) {
        val audioFocus by rememberAudioFocus()
        SanghunComposeVideoVideoPlayer(
            mediaItems = listOf(
                VideoPlayerMediaItem.StorageMediaItem(
                    storageUri = media.uri,
                    mimeType = media.mimeType
                )
            ),
            handleLifecycle = true,
            autoPlay = playWhenReady,
            usePlayerController = false,
            enablePip = false,
            handleAudioFocus = audioFocus,
            repeatMode = RepeatMode.ONE,
            playerInstance = {
                exoPlayer = this
                addListener(
                    object : Player.Listener {
                        override fun onEvents(player: Player, events: Player.Events) {
                            totalDuration = duration.coerceAtLeast(0L)
                            lastPlayingState = isPlaying.value
                            isPlaying.value = player.isPlaying
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onItemClick,
                )
                .swipe(
                    onSwipeDown = onSwipeDown
                ),
        )
    }
    AnimatedVisibility(
        visible = exoPlayer != null,
        enter = enterAnimation,
        exit = exitAnimation
    ) {
        LaunchedEffect(isPlaying.value) {
            exoPlayer!!.playWhenReady = isPlaying.value
        }
        if (isPlaying.value) {
            LaunchedEffect(Unit) {
                while (true) {
                    bufferedPercentage = exoPlayer!!.bufferedPercentage
                    currentTime.longValue = exoPlayer!!.currentPosition
                    delay(1.seconds / 30)
                }
            }
        }
        videoController(
            exoPlayer!!,
            isPlaying,
            currentTime,
            totalDuration,
            bufferedPercentage,
            frameRate
        )
    }
}