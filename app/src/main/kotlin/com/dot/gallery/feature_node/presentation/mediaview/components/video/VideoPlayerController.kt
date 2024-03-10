/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.video

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.PlaybackSpeed
import com.dot.gallery.feature_node.presentation.util.formatMinSec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerController(
    paddingValues: PaddingValues,
    player: ExoPlayer,
    isPlaying: MutableState<Boolean>,
    currentTime: MutableState<Long>,
    totalTime: Long,
    buffer: Int,
    toggleRotate: () -> Unit,
    frameRate: Float
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp)
                .padding(bottom = paddingValues.calculateBottomPadding() + 72.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            var isMuted by rememberSaveable(isPlaying) { mutableStateOf(player.volume == 0f) }
            var currentVolume by rememberSaveable(player) { mutableFloatStateOf(player.volume) }
            LaunchedEffect(LocalConfiguration.current, player.currentMediaItem) {
                player.volume = if (isMuted) 0f else currentVolume
            }
            var auto by rememberSaveable { mutableStateOf(false) }
            var showMenu by rememberSaveable { mutableStateOf(false) }
            var playbackSpeed by rememberSaveable { mutableFloatStateOf(1f) }
            val ctx = LocalContext.current
            val playbackSpeeds = remember {
                listOf(
                    PlaybackSpeed(1f / (frameRate / 30f), ctx.getString(R.string.auto), true),
                    PlaybackSpeed(0.125f, "0.125x"),
                    PlaybackSpeed(0.25f, "0.25x"),
                    PlaybackSpeed(0.5f, "0.5x"),
                    PlaybackSpeed(1f, "1x"),
                    PlaybackSpeed(2f, "2x")
                )
            }
            LaunchedEffect(playbackSpeed) {
                player.setPlaybackSpeed(playbackSpeed)
                showMenu = false
            }

            Box(contentAlignment = Alignment.TopEnd) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = {
                        showMenu = false
                    }
                ) {
                    playbackSpeeds.forEach { speed ->
                        DropdownMenuItem(
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = {
                                playbackSpeed = speed.speed
                                auto = speed.isAuto
                            },
                            leadingIcon = {
                                RadioButton(
                                    selected = playbackSpeed == speed.speed && !speed.isAuto,
                                    onClick = {
                                        playbackSpeed = speed.speed
                                        auto = speed.isAuto
                                    }
                                )
                            },
                            text = { Text(text = speed.label) }
                        )
                    }
                }
                IconButton(
                    onClick = {
                        showMenu = !showMenu
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        tint = Color.White,
                        contentDescription = stringResource(R.string.change_playback_speed_cd)
                    )
                }
            }
            IconButton(
                onClick = {
                    if (isMuted) {
                        player.volume = currentVolume
                        isMuted = false
                    } else {
                        currentVolume = player.volume
                        player.volume = 0f
                        isMuted = true
                    }
                }
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeMute else Icons.AutoMirrored.Outlined.VolumeUp,
                    tint = Color.White,
                    contentDescription = stringResource(
                        R.string.toggle_audio_cd
                    )
                )
            }
            IconButton(
                onClick = { toggleRotate() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ScreenRotation,
                    tint = Color.White,
                    contentDescription = stringResource(
                        R.string.rotate_screen_cd
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    modifier = Modifier.width(52.dp),
                    text = currentTime.value.formatMinSec(),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Box(Modifier.weight(1f)) {
                    Slider(
                        modifier = Modifier.fillMaxWidth(),
                        value = buffer.toFloat(),
                        enabled = false,
                        onValueChange = {},
                        valueRange = 0f..100f,
                        colors =
                        SliderDefaults.colors(
                            disabledThumbColor = Color.Transparent,
                            disabledInactiveTrackColor = Color.DarkGray.copy(alpha = 0.4f),
                            disabledActiveTrackColor = Color.Gray
                        )
                    )
                    Slider(
                        modifier = Modifier.fillMaxWidth(),
                        value = currentTime.value.toFloat(),
                        onValueChange = {
                            scope.launch {
                                currentTime.value = it.toLong()
                                player.seekTo(it.toLong())
                                delay(50)
                                player.play()
                            }
                        },
                        valueRange = 0f..totalTime.toFloat(),
                        colors =
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            activeTickColor = Color.White,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }
                Text(
                    modifier = Modifier.width(52.dp),
                    text = totalTime.formatMinSec(),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        IconButton(
            onClick = { isPlaying.value = !isPlaying.value },
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
        ) {
            if (isPlaying.value) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Filled.PauseCircleFilled,
                    contentDescription = stringResource(R.string.pause_video),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            } else {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Filled.PlayCircleFilled,
                    contentDescription = stringResource(R.string.play_video),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
    }
}