/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.settings.subsettings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dot.gallery.core.encryption.EncryptionStateMonitor
import com.dot.gallery.core.sandbox.PrivateFolderManager
import com.dot.gallery.core.security.AdvancedProtectionMonitor
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.feature.settings.ui.SettingsSecurityActions
import com.dot.gallery.feature.settings.ui.SettingsSecurityContent
import com.dot.gallery.feature.settings.ui.SettingsSecurityState
import com.dot.gallery.feature.help.domain.SettingsSearchRegistry
import com.dot.gallery.core.presentation.util.Screen
import kotlinx.coroutines.launch
import com.dot.gallery.core.ui.settings.rememberPreference

@Composable
fun SettingsSecurityScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var privateFolderUri by rememberPreference(SettingsKeys.privateFolderUri)
    val encryptionState by EncryptionStateMonitor.state.collectAsState()
    val advancedProtection by AdvancedProtectionMonitor.enabled.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                PrivateFolderManager.onFolderPicked(context, it)
                privateFolderUri = it.toString()
            }
        }
    }

    SettingsSecurityContent(
        state = SettingsSecurityState(
            advancedProtection = advancedProtection,
            encryptionState = encryptionState,
        ),
        actions = SettingsSecurityActions(
            onPickPrivateFolder = { folderPickerLauncher.launch(null) },
            onClearPrivateFolder = {
                scope.launch { PrivateFolderManager.clearFolder(context) }
            },
            onClearPrivateFolderAndReveal = {
                scope.launch { PrivateFolderManager.clearFolderAndReveal(context) }
            },
        ),
        searchRoute = Screen.SettingsSecurityScreen(),
        searchSink = SettingsSearchRegistry::register,
    )
}
