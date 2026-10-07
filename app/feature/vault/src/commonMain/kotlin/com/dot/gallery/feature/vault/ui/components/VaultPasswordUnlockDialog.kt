/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.core.util.rememberFeedbackManager
import com.dot.gallery.feature.vault.domain.model.VaultAuthType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.vault.generated.resources.Res
import com.dot.gallery.feature.vault.generated.resources.action_cancel
import com.dot.gallery.feature.vault.generated.resources.toggle_password_visibility_cd
import com.dot.gallery.feature.vault.generated.resources.vault_draw_pattern
import com.dot.gallery.feature.vault.generated.resources.vault_enter_password
import com.dot.gallery.feature.vault.generated.resources.vault_enter_pin
import com.dot.gallery.feature.vault.generated.resources.vault_unlock

/**
 * Top-level unlock dialog that delegates to the correct input based on [authType].
 * Falls back to password mode when type is null (legacy).
 */
@Composable
fun VaultPasswordUnlockDialog(
    authType: VaultAuthType? = null,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (secret: String) -> Unit,
    errorMessage: String? = null
) {
    when (authType ?: VaultAuthType.PASSWORD) {
        VaultAuthType.PIN -> VaultPinUnlockDialog(subtitle, onDismiss, onSubmit, errorMessage)
        VaultAuthType.PATTERN -> VaultPatternUnlockDialog(subtitle, onDismiss, onSubmit, errorMessage)
        VaultAuthType.PASSWORD -> VaultTextPasswordUnlockDialog(subtitle, onDismiss, onSubmit, errorMessage)
    }
}

/**
 * Bottom-sheet wrapper around [VaultPasswordUnlockDialog]. Presents the type-aware unlock
 * UI inside a [ModalBottomSheet] so it does not overlap the underlying screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultPasswordUnlockSheet(
    state: AppBottomSheetState,
    authType: VaultAuthType? = null,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (secret: String) -> Unit,
    errorMessage: String? = null
) {
    val scope = rememberCoroutineScope()
    if (state.isVisible) {
        ModalBottomSheet(
            sheetState = state.sheetState,
            onDismissRequest = {
                scope.launch { state.hide() }
                onDismiss()
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle() },
            sheetMaxWidth = Dp.Unspecified,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            VaultPasswordUnlockDialog(
                authType = authType,
                subtitle = subtitle,
                onDismiss = {
                    scope.launch { state.hide() }
                    onDismiss()
                },
                onSubmit = onSubmit,
                errorMessage = errorMessage
            )
        }
    }
}

@Composable
private fun VaultTextPasswordUnlockDialog(
    subtitle: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    errorMessage: String? = null
) {
    val feedbackManager = rememberFeedbackManager()
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            feedbackManager.vibrateStrong()
            password = ""
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource(Res.string.vault_unlock),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle ?: stringResource(Res.string.vault_enter_password),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(Res.string.vault_enter_password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (password.isNotEmpty()) {
                        feedbackManager.vibrate()
                        onSubmit(password)
                    }
                }
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = stringResource(Res.string.toggle_password_visibility_cd)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        VaultErrorText(errorMessage)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            SetupButton(
                onClick = {
                    feedbackManager.vibrate()
                    onDismiss()
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.action_cancel)
            )
            SetupButton(
                onClick = {
                    feedbackManager.vibrate()
                    onSubmit(password)
                },
                enabled = password.isNotEmpty(),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.vault_unlock)
            )
        }
    }
}

@Composable
private fun VaultPinUnlockDialog(
    subtitle: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    errorMessage: String? = null
) {
    val feedbackManager = rememberFeedbackManager()
    var pin by remember { mutableStateOf("") }
    val isError = errorMessage != null

    LaunchedEffect(isError) {
        if (isError) {
            feedbackManager.vibrateStrong()
            pin = ""
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.vault_unlock),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle ?: stringResource(Res.string.vault_enter_pin),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        VaultPinInput(
            pin = pin,
            isError = isError && pin.isEmpty(),
            onPinChange = { pin = it },
            onPinComplete = onSubmit
        )
        VaultErrorText(errorMessage)
        Spacer(modifier = Modifier.weight(1f))
        SetupButton(
            onClick = {
                feedbackManager.vibrate()
                onDismiss()
            },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            applyHorizontalPadding = false,
            applyBottomPadding = false,
            applyInsets = false,
            text = stringResource(Res.string.action_cancel)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun VaultPatternUnlockDialog(
    subtitle: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    errorMessage: String? = null
) {
    val feedbackManager = rememberFeedbackManager()
    val isError = errorMessage != null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.vault_unlock),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle ?: stringResource(Res.string.vault_draw_pattern),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        VaultPatternLock(
            isError = isError,
            onPatternComplete = onSubmit
        )
        VaultErrorText(errorMessage)
        Spacer(modifier = Modifier.weight(1f))
        SetupButton(
            onClick = {
                feedbackManager.vibrate()
                onDismiss()
            },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            applyHorizontalPadding = false,
            applyBottomPadding = false,
            applyInsets = false,
            text = stringResource(Res.string.action_cancel)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
