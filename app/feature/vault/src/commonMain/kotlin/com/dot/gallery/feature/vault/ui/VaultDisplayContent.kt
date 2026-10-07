/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.presentation.components.ModalSheet
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.OptionItem
import com.dot.gallery.core.presentation.components.OptionSheet
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.domain.model.Vault
import com.dot.gallery.domain.model.VaultState
import com.dot.gallery.feature.vault.domain.model.VaultAuthType
import com.dot.gallery.feature.vault.ui.components.DeleteVaultSheet
import com.dot.gallery.feature.vault.ui.components.RemovePasswordSheet
import com.dot.gallery.feature.vault.ui.components.RestoreVaultSheet
import com.dot.gallery.feature.vault.ui.components.SelectVaultSheet
import com.dot.gallery.feature.vault.ui.components.VaultPasswordSetupSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.vault.generated.resources.Res
import com.dot.gallery.feature.vault.generated.resources.action_cancel
import com.dot.gallery.feature.vault.generated.resources.add_media_to_vault_cd
import com.dot.gallery.feature.vault.generated.resources.decrypt_vault
import com.dot.gallery.feature.vault.generated.resources.delete_vault
import com.dot.gallery.feature.vault.generated.resources.encrypting_media
import com.dot.gallery.feature.vault.generated.resources.more_options
import com.dot.gallery.feature.vault.generated.resources.switch_vault_cd
import com.dot.gallery.feature.vault.generated.resources.unknown_vault
import com.dot.gallery.feature.vault.generated.resources.vault_actions
import com.dot.gallery.feature.vault.generated.resources.vault_change_custom_password
import com.dot.gallery.feature.vault.generated.resources.vault_custom_password
import com.dot.gallery.feature.vault.generated.resources.vault_password_removed
import com.dot.gallery.feature.vault.generated.resources.vault_password_set
import com.dot.gallery.feature.vault.generated.resources.vault_remove_custom_password

/**
 * Vault media display chrome: top bar (vault switcher + actions), add-media FAB,
 * encryption-progress sheet and the vault management sheets.
 *
 * The media grid itself is supplied by the host through [gridContent]
 * (pinch-zoom + [com.dot.gallery.core.presentation.components.MediaGridView]
 * wiring, haze source), and the selection toolbar through [selectionContent]
 * — both stay app-side while the shared grid chrome settles.
 *
 * Platform operations are callbacks: media picking ([onAddMediaClick]), credential
 * persistence ([onSecretSet]/[onRemovePassword]), and vault mutations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDisplayContent(
    vaultState: VaultState,
    currentVault: Vault?,
    isSelectionActive: Boolean,
    workerIsRunning: Boolean,
    workerProgress: Float,
    pendingEncryptCount: Int,
    hasCustomPassword: Boolean,
    canScroll: Boolean,
    modifier: Modifier = Modifier,
    userMessage: Flow<String>? = null,
    onNavigateUp: () -> Unit,
    onVaultDeleted: () -> Unit,
    onResyncVault: (Vault) -> Unit,
    onAuthenticateVault: (Vault) -> Unit,
    onDeleteVault: (Vault) -> Unit,
    onRestoreVault: (Vault) -> Unit,
    onSecretSet: suspend (type: VaultAuthType, secret: String) -> Unit,
    onRemovePassword: suspend () -> Unit,
    onAddMediaClick: () -> Unit,
    onEncryptSheetCancel: (dropCount: Int) -> Unit,
    gridContent: @Composable (PaddingValues) -> Unit,
    selectionContent: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()

    // Keep current vault in sync; if deleted or null, navigate back
    LaunchedEffect(vaultState, currentVault) {
        val current = currentVault
        val vaults = vaultState.vaults
        if (current != null && vaults.any { it.uuid == current.uuid }) {
            onResyncVault(current)
        } else if (current == null && vaults.isNotEmpty()) {
            // Vault was deleted but others remain — go to vault selector for auth
            onVaultDeleted()
        } else if (current == null && vaults.isEmpty()) {
            // Last vault deleted — exit to library
            onNavigateUp()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { canScroll },
    )

    val bottomSheetState = rememberAppBottomSheetState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(userMessage) {
        userMessage?.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val passwordSetupSheetState = rememberAppBottomSheetState()
    val passwordSetMsg = stringResource(Res.string.vault_password_set)
    val passwordRemovedMsg = stringResource(Res.string.vault_password_removed)
    val removePasswordSheetState = rememberAppBottomSheetState()
    RemovePasswordSheet(
        state = removePasswordSheetState,
        onConfirm = {
            if (currentVault == null) return@RemovePasswordSheet
            scope.launch {
                onRemovePassword()
                snackbarHostState.showSnackbar(passwordRemovedMsg)
            }
        }
    )
    VaultPasswordSetupSheet(
        state = passwordSetupSheetState,
        onSecretSet = { type, secret ->
            if (currentVault == null) return@VaultPasswordSetupSheet
            scope.launch {
                onSecretSet(type, secret)
                snackbarHostState.showSnackbar(passwordSetMsg)
            }
        }
    )

    val decryptVaultSheetState = rememberAppBottomSheetState()
    val deleteVaultSheetState = rememberAppBottomSheetState()
    val actionsSheetState = rememberAppBottomSheetState()

    LaunchedEffect(vaultState) {
        if (vaultState.isLoading) return@LaunchedEffect
        if (vaultState.vaults.isNotEmpty()) return@LaunchedEffect
        onNavigateUp()
    }

    LaunchedEffect(workerIsRunning) {
        if (workerIsRunning) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    ModalSheet(
        sheetState = bottomSheetState,
        onDismissRequest = {},
        title = stringResource(Res.string.encrypting_media),
        content = {
            Box(
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = {
                        workerProgress.fastCoerceAtLeast(0f)
                    },
                    strokeWidth = 4.dp,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.size(128.dp),
                )
                Text(text = "${workerProgress.roundToInt()}%")
            }

            SetupButton(
                onClick = {
                    scope.launch {
                        val indexesToDrop =
                            (workerProgress * pendingEncryptCount / 100).roundToInt()
                        onEncryptSheetCancel(indexesToDrop)
                        bottomSheetState.hide()
                    }
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                text = stringResource(Res.string.action_cancel)
            )
        }
    )

    Box(
        modifier = modifier
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !isSelectionActive,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    FloatingActionButton(
                        onClick = {
                            onAddMediaClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(Res.string.add_media_to_vault_cd)
                        )
                    }
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            val sheetState = rememberAppBottomSheetState()
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        enabled = vaultState.vaults.size > 1,
                                    ) {
                                        scope.launch {
                                            sheetState.show()
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = currentVault?.name
                                        ?: stringResource(Res.string.unknown_vault)
                                )
                                AnimatedVisibility(
                                    visible = vaultState.vaults.size > 1,
                                    enter = enterAnimation,
                                    exit = exitAnimation
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = CircleShape
                                            ),
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = stringResource(Res.string.switch_vault_cd),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            SelectVaultSheet(
                                state = sheetState,
                                vaultState = vaultState,
                                excludeVault = currentVault,
                                requireGateAuth = false
                            ) { vault ->
                                onAuthenticateVault(vault)
                            }
                        }
                    },
                    navigationIcon = {
                        NavigationBackButton(
                            forcedAction = onNavigateUp,
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch { actionsSheetState.show() }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(Res.string.more_options)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { scaffoldPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                gridContent(scaffoldPadding)
            }
        }

        selectionContent()

        DeleteVaultSheet(
            state = deleteVaultSheetState,
            vaultName = currentVault?.name
        ) {
            val vault = currentVault ?: vaultState.vaults.firstOrNull()
            vault?.let { onDeleteVault(it) }
        }
        RestoreVaultSheet(
            state = decryptVaultSheetState
        ) {
            val vault = currentVault ?: vaultState.vaults.firstOrNull()
            vault?.let { onRestoreVault(it) }
        }
        VaultActionsSheet(
            state = actionsSheetState,
            hasCustomPassword = hasCustomPassword,
            onDecryptVault = { scope.launch { decryptVaultSheetState.show() } },
            onCustomPassword = {
                if (hasCustomPassword) {
                    scope.launch { removePasswordSheetState.show() }
                } else {
                    scope.launch { passwordSetupSheetState.show() }
                }
            },
            onChangePassword = { scope.launch { passwordSetupSheetState.show() } },
            onDeleteVault = { scope.launch { deleteVaultSheetState.show() } }
        )
    }
}

@Composable
private fun VaultActionsSheet(
    state: AppBottomSheetState,
    hasCustomPassword: Boolean,
    onDecryptVault: () -> Unit,
    onCustomPassword: () -> Unit,
    onChangePassword: () -> Unit,
    onDeleteVault: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val decryptLabel = stringResource(Res.string.decrypt_vault)
    val customPasswordLabel = stringResource(
        if (hasCustomPassword) Res.string.vault_remove_custom_password
        else Res.string.vault_custom_password
    )
    val changePasswordLabel = stringResource(Res.string.vault_change_custom_password)
    val deleteVaultLabel = stringResource(Res.string.delete_vault)

    val options = remember(
        hasCustomPassword,
        decryptLabel,
        customPasswordLabel,
        changePasswordLabel,
        deleteVaultLabel
    ) {
        buildList {
            add(
                OptionItem(
                    icon = Icons.Outlined.Restore,
                    text = decryptLabel,
                    onClick = {
                        scope.launch { state.hide() }
                        onDecryptVault()
                    }
                ))
            if (hasCustomPassword) {
                add(
                    OptionItem(
                        icon = Icons.Outlined.Lock,
                        text = changePasswordLabel,
                        onClick = {
                            scope.launch { state.hide() }
                            onChangePassword()
                        }
                    ))
            }
            add(
                OptionItem(
                    icon = if (hasCustomPassword) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    text = customPasswordLabel,
                    onClick = {
                        scope.launch { state.hide() }
                        onCustomPassword()
                    }
                ))
            add(
                OptionItem(
                    icon = Icons.Filled.DeleteOutline,
                    text = deleteVaultLabel,
                    onClick = {
                        scope.launch { state.hide() }
                        onDeleteVault()
                    }
                ))
        }.toMutableStateList()
    }

    OptionSheet(
        state = state,
        headerContent = {
            Text(
                text = stringResource(Res.string.vault_actions),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        },
        optionList = arrayOf(options)
    )
}
