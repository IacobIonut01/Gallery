/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen

@Composable
fun CloudViewerSettingsScreen(configId: Long) {
    val settingsVm = hiltViewModel<CloudSettingsViewModel>()
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()

    LaunchedEffect(configId) { settingsVm.loadConfig(configId) }
    if (config == null) {
        AccountSettingsStateScreen(stringResource(R.string.cloud_viewer), loadState)
        return
    }
    val loopVideos = config?.loopVideos ?: false
    val videosHeader = stringResource(R.string.cloud_viewer_videos)
    val loopTitle = stringResource(R.string.cloud_viewer_loop)
    val loopSummary = stringResource(R.string.cloud_viewer_loop_summary)

    val settingsList = remember(loopVideos, videosHeader, loopTitle, loopSummary) {
        buildList {
            // Looping is currently the only per-cloud viewer option consumed by playback. The
            // preview/original/autoplay/force-original controls were removed rather than presenting
            // switches that did not affect either Sketch or ExoPlayer.
            add(SettingsEntity.Header(title = videosHeader))
            add(
                SettingsEntity.SwitchPreference(
                    title = loopTitle,
                    summary = loopSummary,
                    isChecked = loopVideos,
                    onCheck = { settingsVm.updateConfig { copy(loopVideos = it) } },
                    screenPosition = Position.Alone
                )
            )
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(R.string.cloud_viewer),
        settingsList = settingsList
    )
}
