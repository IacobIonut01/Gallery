/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.cloud.ui.settings.AccountSettingsStateScreen
import com.dot.gallery.cloud.ui.settings.CloudSettingsViewModel
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_backup_options


@Composable
fun BackupOptionsScreen(configId: Long) {
    val settingsVm = injectViewModel<CloudSettingsViewModel> {cloudSettingsViewModel}
    val config by settingsVm.config.collectAsStateWithLifecycle()
    val loadState by settingsVm.loadState.collectAsStateWithLifecycle()
    val global = configId <= 0L

    LaunchedEffect(configId) { settingsVm.loadConfig(configId, allowGlobal = true) }
    val loaded = config
    if (loaded == null) {
        AccountSettingsStateScreen(stringResource(Res.string.cloud_backup_options), loadState)
        return
    }
    BackupOptionsContent(
        config = loaded,
        global = global,
        onUpdateConfig = { transform -> settingsVm.updateConfig(transform) },
    )
}
