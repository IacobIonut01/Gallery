/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.core.presentation.components.AppTextField
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.SetupButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudAddServerScreen(
    providerType: ProviderType,
    configId: Long? = null,
    onSaved: () -> Unit = {},
    viewModel: CloudAccountsViewModel = hiltViewModel()
) {
    val state by viewModel.addServerState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isEditMode = configId != null && configId > 0
    val urlPattern = remember { Regex("^https?://.+") }
    val isUrlValid = state.serverUrl.isBlank() || urlPattern.matches(state.serverUrl)
    val hasRequiredCredentials = when (state.providerType) {
        ProviderType.IMMICH -> state.apiKey.isNotBlank() || (state.username.isNotBlank() && state.password.isNotBlank())
        ProviderType.OWNCLOUD -> state.username.isNotBlank() && state.password.isNotBlank()
        else -> true
    }
    val canSave = state.serverUrl.isNotBlank() && isUrlValid && hasRequiredCredentials && !state.isSaving

    LaunchedEffect(providerType, configId) {
        if (configId != null && configId > 0) {
            viewModel.initEditServer(configId)
        } else {
            viewModel.initAddServer(providerType)
        }
    }

    val syncCompleted by viewModel.syncCompleted.collectAsStateWithLifecycle()
    LaunchedEffect(syncCompleted) {
        if (syncCompleted) {
            onSaved()
        }
    }

    val title = if (isEditMode) {
        stringResource(R.string.cloud_credentials)
    } else {
        stringResource(R.string.cloud_add_server)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = { NavigationBackButton() },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            AppTextField(
                value = state.serverUrl,
                onValueChange = viewModel::updateServerUrl,
                label = { Text(stringResource(R.string.cloud_server_url)) },
                placeholder = { Text(stringResource(R.string.cloud_server_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.serverUrl.isNotBlank() && !isUrlValid,
                supportingText = if (state.serverUrl.isNotBlank() && !isUrlValid) {
                    { Text(stringResource(R.string.cloud_url_invalid)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            AppTextField(
                value = state.displayName,
                onValueChange = viewModel::updateDisplayName,
                label = { Text(stringResource(R.string.cloud_display_name)) },
                placeholder = { Text(stringResource(R.string.cloud_display_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (state.providerType == ProviderType.IMMICH) {
                AppTextField(
                    value = state.apiKey,
                    onValueChange = viewModel::updateApiKey,
                    label = { Text(stringResource(R.string.cloud_api_key)) },
                    placeholder = { Text(stringResource(R.string.cloud_api_key_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            if (state.providerType == ProviderType.OWNCLOUD ||
                (state.providerType == ProviderType.IMMICH && state.apiKey.isBlank())
            ) {
                AppTextField(
                    value = state.username,
                    onValueChange = viewModel::updateUsername,
                    label = {
                        Text(
                            stringResource(
                                if (state.providerType == ProviderType.IMMICH) R.string.cloud_email
                                else R.string.cloud_username
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (state.providerType == ProviderType.IMMICH) KeyboardType.Email
                        else KeyboardType.Text
                    )
                )
                AppTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text(stringResource(R.string.cloud_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            // Test connection
            SetupButton(
                text = if (state.isTesting) stringResource(R.string.cloud_testing)
                       else stringResource(R.string.cloud_test_connection),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                enabled = !state.isTesting && state.serverUrl.isNotBlank(),
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                onClick = viewModel::testConnection
            )

            state.testResult?.let { result ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (state.testSuccess) Icons.Default.Check else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (state.testSuccess) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Text(
                        result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.testSuccess) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Sync settings (only in add mode)
            if (!isEditMode) {
                Text(
                    stringResource(R.string.cloud_sync_settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.cloud_sync_enabled))
                    Switch(
                        checked = state.syncEnabled,
                        onCheckedChange = viewModel::updateSyncEnabled
                    )
                }

                AnimatedVisibility(visible = state.syncEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.cloud_sync_wifi_only))
                        Switch(
                            checked = state.wifiOnly,
                            onCheckedChange = viewModel::updateWifiOnly
                        )
                    }
                }
            }

            // Save
            Spacer(Modifier.height(8.dp))
            SetupButton(
                text = if (state.isSaving) stringResource(R.string.cloud_save) + "…"
                       else stringResource(R.string.cloud_save),
                enabled = canSave,
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                onClick = { viewModel.saveServer() }
            )

            state.error?.let { error ->
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

}
