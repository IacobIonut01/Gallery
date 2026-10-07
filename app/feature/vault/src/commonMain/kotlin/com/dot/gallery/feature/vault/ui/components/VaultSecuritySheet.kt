/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.OptionItem
import com.dot.gallery.core.presentation.components.OptionLayout
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.domain.model.Vault
import com.dot.gallery.feature.vault.domain.model.VaultAuthType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.vault.generated.resources.Res
import com.dot.gallery.feature.vault.generated.resources.vault_change_security
import com.dot.gallery.feature.vault.generated.resources.vault_change_security_summary
import com.dot.gallery.feature.vault.generated.resources.vault_manage_security
import com.dot.gallery.feature.vault.generated.resources.vault_remove_custom_security
import com.dot.gallery.feature.vault.generated.resources.vault_remove_custom_security_summary
import com.dot.gallery.feature.vault.generated.resources.vault_set_password

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSecuritySheet(
    state: AppBottomSheetState,
    vault: Vault?,
    currentAuthType: VaultAuthType?,
    onSetCustomSecurity: () -> Unit,
    onRemoveCustomSecurity: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    if (state.isVisible && vault != null) {
        ModalBottomSheet(
            sheetState = state.sheetState,
            onDismissRequest = {
                scope.launch { state.hide() }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(Res.string.vault_manage_security),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                )

                val removeText = stringResource(Res.string.vault_remove_custom_security)
                val removeSummary = stringResource(Res.string.vault_remove_custom_security_summary)
                val setPasswordText = stringResource(Res.string.vault_set_password)
                val changeText = stringResource(Res.string.vault_change_security)
                val changeSummary = stringResource(Res.string.vault_change_security_summary)

                val options = remember(currentAuthType, removeText, removeSummary, setPasswordText, changeText, changeSummary) {
                    buildList {
                        if (currentAuthType != null) {
                            add(
                                OptionItem(
                                    icon = Icons.Outlined.LockOpen,
                                    text = removeText,
                                    summary = removeSummary,
                                    onClick = { onRemoveCustomSecurity() }
                                )
                            )
                        }
                        add(
                            OptionItem(
                                icon = if (currentAuthType != null) Icons.Outlined.Password else Icons.Outlined.Lock,
                                text = if (currentAuthType != null) setPasswordText else changeText,
                                summary = changeSummary,
                                onClick = { onSetCustomSecurity() }
                            )
                        )
                    }.toMutableStateList()
                }

                OptionLayout(
                    modifier = Modifier.fillMaxWidth(),
                    optionList = options
                )
            }
        }
    }
}
