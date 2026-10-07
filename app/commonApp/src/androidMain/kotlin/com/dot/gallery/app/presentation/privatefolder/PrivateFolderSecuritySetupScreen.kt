/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.privatefolder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dot.gallery.core.presentation.util.LocalHazeState
import com.dot.gallery.feature.vault.domain.model.GateMode
import com.dot.gallery.feature.vault.ui.PrivateFolderSecuritySetupContent
import com.dot.gallery.feature.vault.utils.VaultPasswordManager
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

/**
 * Private-folder security setup screen: pick none / device security / custom
 * secret. UI lives in `:feature:vault` ([PrivateFolderSecuritySetupContent]);
 * gate-mode and credential persistence run through [VaultPasswordManager] here.
 */
@Composable
fun PrivateFolderSecuritySetupScreen(
    onBack: (() -> Unit)? = null,
    onNone: () -> Unit,
    onDeviceSecurity: () -> Unit,
    onCustomComplete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    PrivateFolderSecuritySetupContent(
        modifier = Modifier.hazeSource(LocalHazeState.current),
        onBack = onBack,
        onNoneSelected = {
            scope.launch {
                VaultPasswordManager.setPrivateFolderMode(context, GateMode.NONE)
                onNone()
            }
        },
        onDeviceSecuritySelected = {
            scope.launch {
                VaultPasswordManager.setPrivateFolderMode(context, GateMode.DEVICE)
                onDeviceSecurity()
            }
        },
        onCustomSecretSet = { type, secret ->
            scope.launch {
                VaultPasswordManager.setPrivateFolderMode(context, GateMode.CUSTOM)
                VaultPasswordManager.setPassword(
                    context,
                    VaultPasswordManager.PRIVATE_FOLDER_UUID,
                    secret,
                    type
                )
                onCustomComplete()
            }
        }
    )
}
