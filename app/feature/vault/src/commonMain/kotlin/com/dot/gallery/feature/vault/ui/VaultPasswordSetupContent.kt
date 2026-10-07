/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.dot.gallery.feature.vault.generated.resources.vault_custom_password_summary
import com.dot.gallery.feature.vault.generated.resources.vault_set_password
import com.dot.gallery.feature.vault.generated.resources.vault_setup_password_prompt
import com.dot.gallery.feature.vault.generated.resources.vault_skip_password

/**
 * Second vault-setup step: optional custom password. [onSecretSet] receives the
 * chosen auth type + secret; the host persists it (credential store on Android).
 */
@Composable
fun VaultPasswordSetupContent(
    modifier: Modifier = Modifier,
    onSkip: () -> Unit,
    onSecretSet: (type: VaultAuthType, secret: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val passwordSetupSheetState = rememberAppBottomSheetState()

    VaultSetupWizard(
        modifier = modifier,
        icon = Icons.Encrypted,
        title = stringResource(Res.string.vault_set_password),
        subtitle = stringResource(Res.string.vault_setup_password_prompt),
        bottomBar = {
            SetupButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                text = stringResource(Res.string.vault_skip_password)
            )
            SetupButton(
                onClick = {
                    scope.launch { passwordSetupSheetState.show() }
                },
                modifier = Modifier.weight(1f),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                text = stringResource(Res.string.vault_set_password)
            )
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.vault_custom_password_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )

    VaultPasswordSetupSheet(
        state = passwordSetupSheetState,
        onSecretSet = onSecretSet
    )
}
