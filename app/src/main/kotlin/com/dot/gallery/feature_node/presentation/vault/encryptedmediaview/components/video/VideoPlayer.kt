/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.vault.encryptedmediaview.components.video

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dot.gallery.BuildConfig
import com.dot.gallery.feature_node.domain.model.EncryptedMedia
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.seconds


fun encryptedMediaToFileDescriptor(encryptedMedia: EncryptedMedia): FileDescriptor {
    // Create a temporary file
    val tempFile = File.createTempFile("${encryptedMedia.id}.temp", null)

    // Write the ByteArray to the temporary file
    FileOutputStream(tempFile).use { fileOutputStream ->
        fileOutputStream.write(encryptedMedia.bytes)
        fileOutputStream.flush()
    }

    // Get the FileDescriptor of the temporary file
    return FileInputStream(tempFile).fd
}

object UriByteDataHelper {
    fun getUri(context: Context, data: ByteArray, extension: String, isVideo: Boolean): Uri {
        // Create a temporary file
        val tempFile =
            File(context.cacheDir, "shared_${if (isVideo) "video" else "image"}.${extension}")

        // Write the ByteArray to the temporary file
        FileOutputStream(tempFile).use { fileOutputStream ->
            fileOutputStream.write(data)
            fileOutputStream.flush()
        }

        // Get the URI of the temporary file using FileProvider
        return FileProvider.getUriForFile(context, BuildConfig.CONTENT_AUTHORITY, tempFile)
    }
}

@SuppressLint("OpaqueUnitKey")
@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    media: EncryptedMedia,
    playWhenReady: Boolean,
    videoController: @Composable (ExoPlayer, MutableState<Boolean>, MutableLongState, Long, Int, Float) -> Unit,
    onItemClick: () -> Unit
) {
    var totalDuration by rememberSaveable { mutableLongStateOf(0L) }
    val currentTime = rememberSaveable { mutableLongStateOf(0L) }
    var bufferedPercentage by rememberSaveable { mutableIntStateOf(0) }
    val isPlaying = rememberSaveable(playWhenReady) { mutableStateOf(playWhenReady) }
    var lastPlayingState by rememberSaveable(isPlaying.value) { mutableStateOf(isPlaying.value) }
    val context = LocalContext.current

    val metadata = remember(media) {
        MediaMetadataRetriever().apply {
            setDataSource(encryptedMediaToFileDescriptor(media))
        }
    }
    val frameRate = remember(metadata) {
        metadata.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
        )?.toFloat() ?: 60f
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build().apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(
                    MediaItem.fromUri(
                        UriByteDataHelper.getUri(
                            context = context,
                            data = media.bytes,
                            extension = media.fileExtension,
                            isVideo = true
                        )
                    )
                )
                prepare()
            }
    }

    val playerView = remember { PlayerView(context) }

    DisposableEffect(
        Box {
            AndroidView(
                modifier = Modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onItemClick,
                    ),
                factory = {
                    playerView.apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

                        player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                        keepScreenOn = true
                    }
                }
            )
            videoController(
                exoPlayer,
                isPlaying,
                currentTime,
                totalDuration,
                bufferedPercentage,
                frameRate
            )
        }
    ) {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                    lastPlayingState = isPlaying.value
                    isPlaying.value = player.isPlaying
                }
            }
        )
        onDispose {
            exoPlayer.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isPlaying.value) {
                exoPlayer.play()
            } else if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(LocalConfiguration.current, isPlaying.value) {
        if (exoPlayer.currentPosition != currentTime.longValue) {
            exoPlayer.seekTo(currentTime.longValue)
        }

        delay(50)

        exoPlayer.playWhenReady = isPlaying.value
    }

    if (isPlaying.value) {
        LaunchedEffect(Unit) {
            while (true) {
                currentTime.longValue = exoPlayer.currentPosition.coerceAtLeast(0L)
                bufferedPercentage = exoPlayer.bufferedPercentage
                delay(1.seconds / 30)
            }
        }
    }

}