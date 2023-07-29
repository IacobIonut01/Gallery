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
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.util.formatMinSec

@Composable
fun VideoPlayerController(
    paddingValues: PaddingValues,
    player: ExoPlayer,
    currentTime: MutableState<Long>,
    totalTime: Long,
    buffer: Int,
    playToggle: () -> Unit,
    toggleRotate: () -> Unit,
) {
    var currentValue by rememberSaveable(currentTime.value) { mutableStateOf(currentTime.value) }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { toggleRotate() },
                modifier = Modifier
                    .align(Alignment.End)
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
                    text = currentValue.formatMinSec(),
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
                        value = currentValue.toFloat(),
                        onValueChange = {
                            currentValue = it.toLong()
                            player.seekTo(it.toLong())
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
            onClick = { playToggle.invoke() },
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
        ) {
            if (player.isPlaying) {
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