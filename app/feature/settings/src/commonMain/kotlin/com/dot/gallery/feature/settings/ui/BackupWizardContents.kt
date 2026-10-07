/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.Saver
import com.dot.gallery.core.backup.BackupSection
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.settings.generated.resources.Res
import com.dot.gallery.feature.settings.generated.resources.backup_back
import com.dot.gallery.feature.settings.generated.resources.backup_choose_file
import com.dot.gallery.feature.settings.generated.resources.backup_encrypt_header
import com.dot.gallery.feature.settings.generated.resources.backup_export_password_notice
import com.dot.gallery.feature.settings.generated.resources.backup_include_header
import com.dot.gallery.feature.settings.generated.resources.backup_include_vaults
import com.dot.gallery.feature.settings.generated.resources.backup_next
import com.dot.gallery.feature.settings.generated.resources.backup_password
import com.dot.gallery.feature.settings.generated.resources.backup_password_confirm
import com.dot.gallery.feature.settings.generated.resources.backup_pick_file_prompt
import com.dot.gallery.feature.settings.generated.resources.backup_protect_with_password
import com.dot.gallery.feature.settings.generated.resources.backup_reading_file
import com.dot.gallery.feature.settings.generated.resources.backup_save_file
import com.dot.gallery.feature.settings.generated.resources.backup_show_password
import com.dot.gallery.feature.settings.generated.resources.backup_warning

enum class ExportStep { SELECT, ENCRYPT }

/** Allows [ExportStep] to be persisted across configuration changes via rememberSaveable. */
val ExportStepSaver = Saver<ExportStep, Int>(
    save = { it.ordinal },
    restore = { ExportStep.entries[it] }
)

/**
 * Export wizard — SELECT step. Vault section toggling is routed through [onToggle] so the
 * caller can intercept the vault gate; [vaultItems]/[vaultIsSelected]/[onToggleVault] drive
 * the per-vault list shown once the section is enabled.
 */
@Composable
fun ExportSelectContent(
    padding: PaddingValues,
    sections: List<BackupSection>,
    isSelected: (BackupSection) -> Boolean,
    onToggle: (BackupSection, Boolean) -> Unit,
    vaultItems: List<Triple<String, String, String?>>,
    vaultIsSelected: (String) -> Boolean,
    onToggleVault: (String, Boolean) -> Unit,
    showVaultList: Boolean,
    nextEnabled: Boolean,
    onNext: () -> Unit,
) {
    BackupScrollColumn(padding) {
        Spacer(Modifier.height(4.dp))
        BackupSectionList(
            sections = sections,
            isSelected = isSelected,
            onToggle = onToggle,
            headerTitle = stringResource(Res.string.backup_include_header)
        )
        if (showVaultList) {
            BackupToggleList(
                items = vaultItems,
                isSelected = vaultIsSelected,
                onToggle = onToggleVault,
                headerTitle = stringResource(Res.string.backup_include_vaults)
            )
        }
        BackupNoticeCard(
            icon = Icons.Outlined.Shield,
            text = stringResource(Res.string.backup_warning)
        )
        BackupPrimaryButton(
            text = stringResource(Res.string.backup_next),
            enabled = nextEnabled,
            onClick = onNext
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Export wizard — ENCRYPT step (optional password protection + save). */
@Composable
fun ExportEncryptContent(
    padding: PaddingValues,
    protect: Boolean,
    onProtectChange: (Boolean) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirm: String,
    onConfirmChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordChange: (Boolean) -> Unit,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    BackupScrollColumn(padding) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.backup_encrypt_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(Res.string.backup_export_password_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = protect, onCheckedChange = onProtectChange)
            Text(
                text = stringResource(Res.string.backup_protect_with_password),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (protect) {
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(Res.string.backup_password)) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = onConfirmChange,
                label = { Text(stringResource(Res.string.backup_password_confirm)) },
                singleLine = true,
                isError = confirm.isNotEmpty() && confirm != password,
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showPassword, onCheckedChange = onShowPasswordChange)
                Text(
                    text = stringResource(Res.string.backup_show_password),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        BackupPrimaryButton(
            text = stringResource(Res.string.backup_save_file),
            enabled = saveEnabled,
            onClick = onSave
        )
        BackupSecondaryButton(
            text = stringResource(Res.string.backup_back),
            onClick = onBack
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Import wizard — pick-a-file prompt shown while no archive is selected. */
@Composable
fun ImportPickingContent(
    padding: PaddingValues,
    onPickFile: () -> Unit,
) {
    BackupCenteredMessage(padding) {
        Text(
            text = stringResource(Res.string.backup_pick_file_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        BackupPrimaryButton(
            text = stringResource(Res.string.backup_choose_file),
            onClick = onPickFile
        )
    }
}

/** Import wizard — indeterminate state while the archive manifest is parsed. */
@Composable
fun ImportInspectingContent(padding: PaddingValues) {
    BackupCenteredMessage(padding) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(Res.string.backup_reading_file))
    }
}
