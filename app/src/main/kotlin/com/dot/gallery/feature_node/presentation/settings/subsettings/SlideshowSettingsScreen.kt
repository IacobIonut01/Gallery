/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.Settings
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.domain.model.SlideshowTransition
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen
import kotlin.math.roundToInt

@Composable
fun SlideshowSettingsScreen() {
    val listState = rememberLazyListState()

    var intervalSeconds by Settings.Slideshow.rememberIntervalSeconds()
    var randomOrder by Settings.Slideshow.rememberRandomOrder()
    var reverseOrder by Settings.Slideshow.rememberReverseOrder()
    var includeGifs by Settings.Slideshow.rememberIncludeGifs()
    var includeVideos by Settings.Slideshow.rememberIncludeVideos()
    var loop by Settings.Slideshow.rememberLoop()
    var transition by Settings.Slideshow.rememberTransition()
    var kenBurns by Settings.Slideshow.rememberKenBurns()

    var showTransitionDialog by rememberSaveable { mutableStateOf(false) }

    @Composable
    fun transitionLabel(value: SlideshowTransition): String = stringResource(
        when (value) {
            SlideshowTransition.FADE -> R.string.slideshow_transition_fade
            SlideshowTransition.SLIDE -> R.string.slideshow_transition_slide
            SlideshowTransition.KEN_BURNS -> R.string.slideshow_transition_ken_burns
        }
    )

    @Composable
    fun settings(): SnapshotStateList<SettingsEntity> {
        val playbackHeader = SettingsEntity.Header(title = stringResource(R.string.slideshow))

        val intervalPref = SettingsEntity.SeekPreference(
            title = stringResource(R.string.slideshow_interval),
            currentValue = intervalSeconds.toFloat(),
            minValue = Settings.Slideshow.MIN_INTERVAL.toFloat(),
            maxValue = Settings.Slideshow.MAX_INTERVAL.toFloat(),
            step = 0,
            valueMultiplier = 1,
            seekSuffix = stringResource(R.string.slideshow_seconds_suffix),
            onSeek = { intervalSeconds = it.roundToInt() },
            screenPosition = Position.Top
        )

        val transitionPref = SettingsEntity.Preference(
            title = stringResource(R.string.slideshow_transition),
            summary = transitionLabel(transition),
            onClick = { showTransitionDialog = true },
            screenPosition = Position.Middle
        )

        val kenBurnsPref = SettingsEntity.SwitchPreference(
            title = stringResource(R.string.slideshow_ken_burns),
            summary = stringResource(R.string.slideshow_ken_burns_summary),
            isChecked = kenBurns,
            onCheck = { kenBurns = it },
            screenPosition = Position.Middle
        )

        val randomPref = SettingsEntity.SwitchPreference(
            title = stringResource(R.string.slideshow_random),
            isChecked = randomOrder,
            onCheck = { randomOrder = it },
            screenPosition = Position.Middle
        )

        val reversePref = SettingsEntity.SwitchPreference(
            title = stringResource(R.string.slideshow_reverse),
            isChecked = reverseOrder,
            enabled = !randomOrder,
            onCheck = { reverseOrder = it },
            screenPosition = Position.Middle
        )

        val loopPref = SettingsEntity.SwitchPreference(
            title = stringResource(R.string.slideshow_loop),
            isChecked = loop,
            onCheck = { loop = it },
            screenPosition = Position.Bottom
        )

        val contentHeader = SettingsEntity.Header(title = stringResource(R.string.slideshow_content_header))

        val includeGifsPref = SettingsEntity.SwitchPreference(
            title = stringResource(R.string.slideshow_include_gifs),
            isChecked = includeGifs,
            onCheck = { includeGifs = it },
            screenPosition = Position.Top
        )

        val includeVideosPref = SettingsEntity.SwitchPreference(
            title = stringResource(R.string.slideshow_include_videos),
            isChecked = includeVideos,
            onCheck = { includeVideos = it },
            screenPosition = Position.Bottom
        )

        return remember(
            intervalPref, transitionPref, kenBurnsPref, randomPref, reversePref,
            loopPref, includeGifsPref, includeVideosPref
        ) {
            mutableStateListOf(
                playbackHeader,
                intervalPref,
                transitionPref,
                kenBurnsPref,
                randomPref,
                reversePref,
                loopPref,
                contentHeader,
                includeGifsPref,
                includeVideosPref
            )
        }
    }

    BaseSettingsScreen(
        title = stringResource(R.string.slideshow),
        settingsList = settings(),
        listState = listState,
    )

    if (showTransitionDialog) {
        AlertDialog(
            onDismissRequest = { showTransitionDialog = false },
            title = { Text(stringResource(R.string.slideshow_transition)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    SlideshowTransition.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = transition == option,
                                    onClick = {
                                        transition = option
                                        showTransitionDialog = false
                                    }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = transition == option,
                                onClick = {
                                    transition = option
                                    showTransitionDialog = false
                                }
                            )
                            Text(
                                text = transitionLabel(option),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTransitionDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}
