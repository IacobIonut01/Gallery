/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
import com.dot.gallery.feature.vault.generated.resources.vault_gate_custom
import com.dot.gallery.feature.vault.generated.resources.vault_gate_device
import com.dot.gallery.feature.vault.generated.resources.vault_gate_lock_all
import com.dot.gallery.feature.vault.generated.resources.vault_gate_lock_all_summary
import com.dot.gallery.feature.vault.generated.resources.vault_gate_none
import com.dot.gallery.feature.vault.generated.resources.vault_gate_setup_subtitle
import com.dot.gallery.feature.vault.generated.resources.vault_gate_setup_summary
import com.dot.gallery.feature.vault.generated.resources.vault_gate_setup_title

/**
 * Gate-setup screen: pick how the vault selector itself is protected (none /
 * device security / custom secret) plus the "one lock for all vaults" toggle.
 * Gate-mode persistence lives in the host — the callbacks fire after the user
 * picks, mirroring the app's ordering (persist, then navigate).
 */
@Composable
fun VaultGateSetupContent(
    lockAllVaults: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNoneSelected: () -> Unit,
    onDeviceSecuritySelected: () -> Unit,
    onCustomSecretSet: (type: VaultAuthType, secret: String) -> Unit,
    onLockAllVaultsChanged: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val customSetupSheetState = rememberAppBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        VaultSetupWizard(
            modifier = modifier,
            icon = Icons.Encrypted,
            title = stringResource(Res.string.vault_gate_setup_title),
            subtitle = stringResource(Res.string.vault_gate_setup_subtitle),
            bottomBar = {},
            content = {
                Text(
                    text = stringResource(Res.string.vault_gate_setup_summary),
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
                        text = stringResource(Res.string.vault_gate_none)
                    )
                    SetupButton(
                        onClick = onDeviceSecuritySelected,
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        text = stringResource(Res.string.vault_gate_device)
                    )
                    SetupButton(
                        onClick = {
                            scope.launch { customSetupSheetState.show() }
                        },
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        text = stringResource(Res.string.vault_gate_custom)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.vault_gate_lock_all),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.vault_gate_lock_all_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = lockAllVaults,
                                onCheckedChange = onLockAllVaultsChanged
                            )
                        }
                    }
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
