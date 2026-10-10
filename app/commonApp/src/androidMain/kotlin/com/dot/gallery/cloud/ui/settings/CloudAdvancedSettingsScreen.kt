/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_advanced


@Composable
fun CloudAdvancedSettingsScreen(configId: Long) {
    val settingsVm = injectViewModel<CloudSettingsViewModel> {cloudSettingsViewModel}
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()
    val cacheClearing by settingsVm.cacheClearing.collectAsStateWithLifecycle()

    LaunchedEffect(configId) { settingsVm.loadConfig(configId) }
    val loaded = config
    if (loaded == null) {
        AccountSettingsStateScreen(stringResource(Res.string.cloud_advanced), loadState)
        return
    }
    CloudAdvancedSettingsContent(
        config = loaded,
        cacheClearing = cacheClearing,
        onUpdateConfig = { transform -> settingsVm.updateConfig(transform) },
        onClearImageCache = { settingsVm.clearImageCache() },
    )
}
