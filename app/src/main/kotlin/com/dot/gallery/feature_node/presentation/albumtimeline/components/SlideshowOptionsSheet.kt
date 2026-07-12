/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albumtimeline.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Settings
import com.dot.gallery.core.navigate
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.domain.model.SlideshowTransition
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideshowOptionsSheet(
    state: AppBottomSheetState,
    canStart: Boolean,
    onStart: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val eventHandler = LocalEventHandler.current

    var intervalSeconds by Settings.Slideshow.rememberIntervalSeconds()
    var randomOrder by Settings.Slideshow.rememberRandomOrder()
    var reverseOrder by Settings.Slideshow.rememberReverseOrder()
    var includeGifs by Settings.Slideshow.rememberIncludeGifs()
    var includeVideos by Settings.Slideshow.rememberIncludeVideos()
    var loop by Settings.Slideshow.rememberLoop()
    var transition by Settings.Slideshow.rememberTransition()
    var kenBurns by Settings.Slideshow.rememberKenBurns()

    if (state.isVisible) {
        ModalBottomSheet(
            sheetState = state.sheetState,
            onDismissRequest = { scope.launch { state.hide() } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.slideshow),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Interval
                Text(
                    text = stringResource(R.string.slideshow_interval_value, intervalSeconds),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = intervalSeconds.toFloat(),
                    onValueChange = { intervalSeconds = it.toInt() },
                    valueRange = Settings.Slideshow.MIN_INTERVAL.toFloat()..Settings.Slideshow.MAX_INTERVAL.toFloat()
                )

                // Transition
                Text(
                    text = stringResource(R.string.slideshow_transition),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    TransitionChip(R.string.slideshow_transition_fade, transition == SlideshowTransition.FADE) {
                        transition = SlideshowTransition.FADE
                    }
                    TransitionChip(R.string.slideshow_transition_slide, transition == SlideshowTransition.SLIDE) {
                        transition = SlideshowTransition.SLIDE
                    }
                    TransitionChip(R.string.slideshow_transition_ken_burns, transition == SlideshowTransition.KEN_BURNS) {
                        transition = SlideshowTransition.KEN_BURNS
                    }
                }

                SlideshowSwitchRow(R.string.slideshow_ken_burns, kenBurns) { kenBurns = it }
                SlideshowSwitchRow(R.string.slideshow_random, randomOrder) { randomOrder = it }
                SlideshowSwitchRow(R.string.slideshow_reverse, reverseOrder, enabled = !randomOrder) {
                    reverseOrder = it
                }
                SlideshowSwitchRow(R.string.slideshow_include_gifs, includeGifs) { includeGifs = it }
                SlideshowSwitchRow(R.string.slideshow_include_videos, includeVideos) { includeVideos = it }
                SlideshowSwitchRow(R.string.slideshow_loop, loop) { loop = it }

                Spacer(Modifier.height(8.dp))
                SetupButton(
                    text = stringResource(R.string.slideshow_start),
                    enabled = canStart,
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    onClick = {
                        scope.launch {
                            state.hide()
                            onStart()
                        }
                    }
                )
                SetupButton(
                    text = stringResource(R.string.slideshow_more_settings),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    onClick = {
                        scope.launch {
                            state.hide()
                            eventHandler.navigate(Screen.SlideshowSettingsScreen())
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransitionChip(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(labelRes)) }
    )
}

@Composable
private fun SlideshowSwitchRow(
    labelRes: Int,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = checked && enabled,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
