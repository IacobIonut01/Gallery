/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.ui.components.BaseSettingsScreen
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.data.entity.CloudServerConfigEntity
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_viewer
import com.dot.gallery.generated.resources.cloud_viewer_loop
import com.dot.gallery.generated.resources.cloud_viewer_loop_summary
import com.dot.gallery.generated.resources.cloud_viewer_videos


/**
 * Stateless per-account viewer options. [CloudViewerSettingsScreen] loads the
 * config and delegates here; the docs capture tier supplies a sample.
 */
@Composable
fun CloudViewerSettingsContent(
    config: CloudServerConfigEntity,
    onUpdateConfig: (CloudServerConfigEntity.() -> CloudServerConfigEntity) -> Unit,
) {
    val loopVideos = config.loopVideos
    val videosHeader = stringResource(Res.string.cloud_viewer_videos)
    val loopTitle = stringResource(Res.string.cloud_viewer_loop)
    val loopSummary = stringResource(Res.string.cloud_viewer_loop_summary)

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
                    onCheck = { onUpdateConfig { copy(loopVideos = it) } },
                    screenPosition = Position.Alone
                )
            )
        }.toMutableStateList()
    }

    BaseSettingsScreen(
        title = stringResource(Res.string.cloud_viewer),
        settingsList = settingsList
    )
}
