/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.feature.vault.domain.model.VaultAuthType
import com.dot.gallery.feature.vault.ui.components.VaultPasswordSetupSheet
import com.dot.gallery.feature.vault.ui.components.VaultSetupWizard
import com.dot.gallery.ui.core.Icons
import com.dot.gallery.ui.core.icons.Encrypted
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.vault.generated.resources.Res
import com.dot.gallery.feature.vault.generated.resources.private_folder_security_custom
import com.dot.gallery.feature.vault.generated.resources.private_folder_security_device
import com.dot.gallery.feature.vault.generated.resources.private_folder_security_none
import com.dot.gallery.feature.vault.generated.resources.private_folder_security_subtitle
import com.dot.gallery.feature.vault.generated.resources.private_folder_security_title

/**
 * Private-folder security setup: pick how the folder is protected (none /
 * device security / custom secret). Mode persistence lives in the host — the
 * callbacks fire after the user picks, mirroring the app's ordering (persist,
 * then navigate).
 */
@Composable
fun PrivateFolderSecuritySetupContent(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onNoneSelected: () -> Unit,
    onDeviceSecuritySelected: () -> Unit,
    onCustomSecretSet: (type: VaultAuthType, secret: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val customSetupSheetState = rememberAppBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        VaultSetupWizard(
            modifier = modifier,
            icon = Icons.Encrypted,
            title = stringResource(Res.string.private_folder_security_title),
            subtitle = stringResource(Res.string.private_folder_security_subtitle),
            bottomBar = {},
            content = {
                Text(
                    text = stringResource(Res.string.private_folder_security_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SetupButton(
                        onClick = onNoneSelected,
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        text = stringResource(Res.string.private_folder_security_none)
                    )
                    SetupButton(
                        onClick = onDeviceSecuritySelected,
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        text = stringResource(Res.string.private_folder_security_device)
                    )
                    SetupButton(
                        onClick = {
                            scope.launch { customSetupSheetState.show() }
                        },
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        text = stringResource(Res.string.private_folder_security_custom)
                    )
                }
            }
        )

        if (onBack != null) {
            NavigationBackButton(
                modifier = Modifier.statusBarsPadding(),
                forcedAction = onBack
            )
        }
    }

    VaultPasswordSetupSheet(
        state = customSetupSheetState,
        onSecretSet = onCustomSecretSet
    )
}
