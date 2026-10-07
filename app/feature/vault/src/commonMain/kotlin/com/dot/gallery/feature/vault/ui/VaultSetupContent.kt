/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.domain.model.Vault
import com.dot.gallery.feature.vault.ui.components.VaultSetupWizard
import com.dot.gallery.ui.core.Icons
import com.dot.gallery.ui.core.icons.Encrypted
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.vault.generated.resources.Res
import com.dot.gallery.feature.vault.generated.resources.action_cancel
import com.dot.gallery.feature.vault.generated.resources.get_started
import com.dot.gallery.feature.vault.generated.resources.vault_already_exists
import com.dot.gallery.feature.vault.generated.resources.vault_setup_decryption_info
import com.dot.gallery.feature.vault.generated.resources.vault_setup_name
import com.dot.gallery.feature.vault.generated.resources.vault_setup_security_error
import com.dot.gallery.feature.vault.generated.resources.vault_setup_subtitle
import com.dot.gallery.feature.vault.generated.resources.vault_setup_summary2
import com.dot.gallery.feature.vault.generated.resources.vault_setup_title

/**
 * First vault-setup step: name entry + device-security notice.
 *
 * [deviceSecurityAvailable] is resolved by the host (BiometricManager/KeyguardManager
 * on Android). [onCreate] receives the newly-named [Vault]; duplicate-name
 * validation against [existingVaults] stays inside the composable.
 */
@Composable
fun VaultSetupContent(
    existingVaults: List<Vault>,
    deviceSecurityAvailable: Boolean,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onCreate: (Vault) -> Unit,
) {
    var nameError by remember { mutableStateOf("") }
    var newVault by remember { mutableStateOf(Vault(name = "")) }
    val alreadyExists = stringResource(Res.string.vault_already_exists, newVault.name)

    VaultSetupWizard(
        modifier = modifier,
        icon = Icons.Encrypted,
        title = stringResource(Res.string.vault_setup_title),
        subtitle = stringResource(Res.string.vault_setup_subtitle),
        bottomBar = {
            SetupButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                text = stringResource(Res.string.action_cancel)
            )
            SetupButton(
                onClick = {
                    val nameExists = existingVaults.any { it.name == newVault.name }
                    if (nameExists) {
                        nameError = alreadyExists
                    } else {
                        onCreate(newVault)
                    }
                },
                enabled = deviceSecurityAvailable && nameError.isEmpty() && newVault.name.isNotEmpty(),
                modifier = Modifier.weight(1f),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                text = stringResource(Res.string.get_started)
            )
        },
        content = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = newVault.name,
                onValueChange = { newName ->
                    nameError = ""
                    newVault = newVault.copy(name = newName.filter { it.isLetterOrDigit() })
                },
                label = { Text(text = stringResource(Res.string.vault_setup_name)) },
                singleLine = true,
                isError = nameError.isNotEmpty(),
                enabled = deviceSecurityAvailable
            )

            AnimatedVisibility(visible = !deviceSecurityAvailable) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    text = stringResource(Res.string.vault_setup_security_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            AnimatedVisibility(visible = deviceSecurityAvailable) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        text = stringResource(Res.string.vault_setup_summary2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        text = stringResource(Res.string.vault_setup_decryption_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            AnimatedVisibility(visible = nameError.isNotEmpty()) {
                Text(
                    text = nameError,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}
