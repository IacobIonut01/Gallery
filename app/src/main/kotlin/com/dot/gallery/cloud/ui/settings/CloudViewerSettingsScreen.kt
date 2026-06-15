/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.BaseSettingsScreen

@Composable
fun CloudViewerSettingsScreen() {
    val settingsVm = hiltViewModel<CloudSettingsViewModel>()
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val loadPreview = config?.loadPreviewImage ?: true
    val loadOriginal = config?.loadOriginalImage ?: false
    val autoPlayVideos = config?.autoPlayVideos ?: true
    val loopVideos = config?.loopVideos ?: false
    val forceOriginalVideo = config?.forceOriginalVideo ?: false

    val settingsList = remember(loadPreview, loadOriginal, autoPlayVideos, loopVideos, forceOriginalVideo) {
        buildList {
            // Photos section
            add(SettingsEntity.Header(title = context.getString(R.string.cloud_viewer_photos)))
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_viewer_load_preview),
                    summary = context.getString(R.string.cloud_viewer_load_preview_summary),
                    isChecked = loadPreview,
                    onCheck = { settingsVm.updateConfig { copy(loadPreviewImage = it) } },
                    screenPosition = Position.Top
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_viewer_load_original),
                    summary = context.getString(R.string.cloud_viewer_load_original_summary),
                    isChecked = loadOriginal,
                    onCheck = { settingsVm.updateConfig { copy(loadOriginalImage = it) } },
                    screenPosition = Position.Bottom
                )
            )

            // Videos section
            add(SettingsEntity.Header(title = context.getString(R.string.cloud_viewer_videos)))
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_viewer_autoplay),
                    summary = context.getString(R.string.cloud_viewer_autoplay_summary),
                    isChecked = autoPlayVideos,
                    onCheck = { settingsVm.updateConfig { copy(autoPlayVideos = it) } },
                    screenPosition = Position.Top
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_viewer_loop),
                    summary = context.getString(R.string.cloud_viewer_loop_summary),
                    isChecked = loopVideos,
                    onCheck = { settingsVm.updateConfig { copy(loopVideos = it) } },
                    screenPosition = Position.Middle
                )
            )
            add(
                SettingsEntity.SwitchPreference(
                    title = context.getString(R.string.cloud_viewer_force_original),
                    summary = context.getString(R.string.cloud_viewer_force_original_summary),
                    isChecked = forceOriginalVideo,
                    onCheck = { settingsVm.updateConfig { copy(forceOriginalVideo = it) } },
                    screenPosition = Position.Bottom
                )
            )
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(R.string.cloud_viewer),
        settingsList = settingsList
    )
}
