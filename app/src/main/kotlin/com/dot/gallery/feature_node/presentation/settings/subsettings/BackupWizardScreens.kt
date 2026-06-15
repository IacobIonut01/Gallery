/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.core.backup.BackupContents
import com.dot.gallery.core.backup.BackupSection
import com.dot.gallery.core.backup.BackupSelection
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.presentation.vault.VaultViewModel
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.presentation.settings.components.SettingsItem
import com.dot.gallery.feature_node.presentation.settings.components.rememberSwitchPreference
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.feature_node.presentation.util.printError
import com.dot.gallery.feature_node.presentation.vault.components.VaultPasswordUnlockSheet
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.vault.utils.GateMode
import com.dot.gallery.feature_node.presentation.vault.utils.VaultAuthType
import com.dot.gallery.feature_node.presentation.vault.utils.VaultPasswordManager
import com.dot.gallery.feature_node.presentation.vault.utils.VerifyResult
import com.dot.gallery.feature_node.presentation.vault.utils.rememberBiometricState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ordered list of sections shown in the export/import wizards. Cloud accounts
 * (server configurations) are omitted on offline variants that ship without any
 * remote cloud provider, mirroring how the Cloud accounts entry is gated elsewhere.
 */
private val ALL_SECTIONS: List<BackupSection> = buildList {
    add(BackupSection.SETTINGS)
    add(BackupSection.LOCAL_FAVORITES)
    add(BackupSection.CLOUD_FAVORITES)
    if (ProviderType.hasAnyRemoteProvider()) add(BackupSection.CLOUD_CONFIGS)
    add(BackupSection.VAULTS)
}

// =================================================================================================
// Export wizard
// =================================================================================================

private enum class ExportStep { SELECT, ENCRYPT }

/** Allows [ExportStep] to be persisted across configuration changes via rememberSaveable. */
private val ExportStepSaver = Saver<ExportStep, Int>(
    save = { it.ordinal },
    restore = { ExportStep.entries[it] }
)

@Composable
fun SettingsBackupExportScreen(navigateUp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<BackupRestoreViewModel>()
    val vaultViewModel = hiltViewModel<VaultViewModel>()
    val phase by viewModel.exportPhase.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val vaultState by vaultViewModel.vaultState.collectAsStateWithLifecycle()

    var step by rememberSaveable(stateSaver = ExportStepSaver) { mutableStateOf(ExportStep.SELECT) }
    var protect by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var pendingPassword by rememberSaveable { mutableStateOf<String?>(null) }

    // Per-vault export authentication. Because the archive decrypts every selected vault,
    // each one is verified with its OWN credential (PIN/pattern/password), falling back to
    // device auth only for vaults that have no custom password set.
    val unlockSheet = rememberAppBottomSheetState()
    var unlockAuthType by remember { mutableStateOf<VaultAuthType?>(null) }
    var unlockError by remember { mutableStateOf<String?>(null) }
    var vaultQueue by remember { mutableStateOf<List<Vault>>(emptyList()) }
    var currentVault by remember { mutableStateOf<Vault?>(null) }
    var deviceVerified by remember { mutableStateOf(false) }
    var biometricTick by remember { mutableIntStateOf(0) }

    // Enabling the Vaults section reveals the vault list, so it is gated behind the vault
    // selector password (if one is configured) and is OFF by default.
    val revealGateSheet = rememberAppBottomSheetState()
    var revealGateAuthType by remember { mutableStateOf<VaultAuthType?>(null) }
    var revealGateError by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? -> uri?.let { viewModel.startExport(it, pendingPassword) } }

    fun launchSaf() {
        val ext = if (pendingPassword != null) "rfbk" else "zip"
        val name = "refra-backup-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.$ext"
        exportLauncher.launch(name)
    }

    val wrongPasswordStr = stringResource(R.string.vault_wrong_password_attempts)
    val lockedOutStr = stringResource(R.string.vault_locked_out)

    // Advances the per-vault verification queue: prompts the next vault's own credential,
    // uses device auth for vaults without a custom password, and writes the file once all pass.
    fun processVaultQueue() {
        val next = vaultQueue.firstOrNull()
        if (next == null) {
            currentVault = null
            launchSaf()
            return
        }
        currentVault = next
        scope.launch {
            val type = VaultPasswordManager.getAuthType(context, next.uuid)
            when {
                type != null -> {
                    unlockAuthType = type
                    unlockError = null
                    unlockSheet.show()
                }
                // A vault with no custom password is covered by a single device auth.
                deviceVerified -> {
                    vaultQueue = vaultQueue.drop(1)
                    processVaultQueue()
                }
                else -> biometricTick++
            }
        }
    }

    val exportBiometric = rememberBiometricState(
        title = stringResource(R.string.biometric_authentication),
        subtitle = stringResource(R.string.backup_verify_export_subtitle),
        onSuccess = {
            deviceVerified = true
            vaultQueue = vaultQueue.drop(1)
            processVaultQueue()
        },
        onFailed = {
            // User cancelled: abort the whole gate, nothing is exported.
            vaultQueue = emptyList()
            currentVault = null
        }
    )

    // Device auth is imperative, so trigger it from an effect when the queue requests it.
    LaunchedEffect(biometricTick) {
        if (biometricTick > 0 && currentVault != null) {
            if (exportBiometric.isSupported) {
                exportBiometric.authenticate()
            } else {
                // No device security configured; this vault has nothing to verify against.
                vaultQueue = vaultQueue.drop(1)
                processVaultQueue()
            }
        }
    }

    fun beginExport() {
        pendingPassword = if (protect && password.isNotBlank()) password else null
        val selectedVaults = if (selection.vaults) {
            vaultState.vaults.filter { selection.isVaultSelected(it.uuid.toString()) }
        } else emptyList()
        if (selectedVaults.isEmpty()) {
            launchSaf()
            return
        }
        deviceVerified = false
        vaultQueue = selectedVaults
        processVaultQueue()
    }

    // The Vaults section is only offered when at least one vault exists on the device.
    val hasVaults = vaultState.vaults.isNotEmpty()
    val exportSections = ALL_SECTIONS.filter { it != BackupSection.VAULTS || hasVaults }
    val vaultIds = vaultState.vaults.map { it.uuid.toString() }

    // Once vaults have loaded and there are none, make sure the section isn't left selected.
    LaunchedEffect(vaultState.isLoading, hasVaults) {
        if (!vaultState.isLoading && !hasVaults && selection.vaults) {
            viewModel.updateSelection { copy(vaults = false) }
        }
    }

    fun toggleVault(id: String, checked: Boolean) {
        viewModel.updateSelection {
            val current = selectedVaultIds ?: vaultIds.toSet()
            copy(selectedVaultIds = if (checked) current + id else current - id)
        }
    }

    // Vault backup starts OFF; the list is only revealed after passing the selector gate.
    var vaultsDefaultApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!vaultsDefaultApplied) {
            viewModel.updateSelection { copy(vaults = false, selectedVaultIds = null) }
            vaultsDefaultApplied = true
        }
    }

    fun enableVaults() {
        viewModel.updateSelection { copy(vaults = true, selectedVaultIds = null) }
    }

    val revealGateBiometric = rememberBiometricState(
        title = stringResource(R.string.biometric_authentication),
        subtitle = stringResource(R.string.backup_verify_export_subtitle),
        onSuccess = { enableVaults() },
        onFailed = { }
    )

    // Verifies the vault selector credential (if any) before exposing the vault list.
    fun requestRevealVaults() {
        scope.launch {
            when (VaultPasswordManager.getGateMode(context)) {
                GateMode.CUSTOM -> {
                    val type = VaultPasswordManager.getAuthType(context, VaultPasswordManager.GATE_UUID)
                    when {
                        type != null -> {
                            revealGateAuthType = type
                            revealGateError = null
                            revealGateSheet.show()
                        }
                        revealGateBiometric.isSupported -> revealGateBiometric.authenticate()
                        else -> enableVaults()
                    }
                }
                GateMode.DEVICE ->
                    if (revealGateBiometric.isSupported) revealGateBiometric.authenticate()
                    else enableVaults()
                GateMode.NONE -> enableVaults()
            }
        }
    }

    val anySelected = exportSections.any { selection.isOn(it) } &&
        // If only the vaults section is on, at least one vault must remain selected.
        (!selection.vaults || exportSections.any { it != BackupSection.VAULTS && selection.isOn(it) } ||
            vaultIds.any { selection.isVaultSelected(it) })
    val passwordsValid = !protect || (password.isNotBlank() && password == confirm)

    BackupWizardScaffold(title = stringResource(R.string.backup_export)) { padding ->
        when (phase) {
            is BackupRestoreViewModel.ExportPhase.Running ->
                BackupProgressContent(padding, selection, progress, stringResource(R.string.backup_exporting))

            is BackupRestoreViewModel.ExportPhase.Done -> {
                val r = (phase as BackupRestoreViewModel.ExportPhase.Done).result
                BackupStatusContent(
                    padding = padding,
                    success = true,
                    title = stringResource(R.string.backup_result_exported),
                    rows = listOf(
                        stringResource(R.string.backup_include_settings) to "${r.settingsCount}",
                        stringResource(R.string.backup_stat_favorites) to
                            "${r.localFavoritesCount + r.cloudFavoritesCount}",
                        stringResource(R.string.backup_include_cloud_configs) to "${r.cloudConfigsCount}",
                        stringResource(R.string.backup_include_vaults) to
                            stringResource(R.string.backup_vaults_value, r.vaultCount, r.vaultMediaCount)
                    ),
                    note = null,
                    onDone = navigateUp
                )
            }

            is BackupRestoreViewModel.ExportPhase.Failed -> {
                val message = (phase as BackupRestoreViewModel.ExportPhase.Failed).message
                BackupStatusContent(
                    padding = padding,
                    success = false,
                    title = stringResource(R.string.backup_error, message),
                    rows = emptyList(),
                    note = null,
                    onDone = { viewModel.resetExport() },
                    doneLabel = stringResource(R.string.backup_try_again)
                )
            }

            BackupRestoreViewModel.ExportPhase.Configuring -> {
                BackupScrollColumn(padding) {
                    Spacer(Modifier.height(4.dp))
                    if (step == ExportStep.SELECT) {
                        BackupSectionList(
                            sections = exportSections,
                            isSelected = { selection.isOn(it) },
                            onToggle = { section, checked ->
                                if (section == BackupSection.VAULTS) {
                                    if (checked) requestRevealVaults()
                                    else viewModel.updateSelection { copy(vaults = false) }
                                } else {
                                    viewModel.updateSelection { with(section, checked) }
                                }
                            },
                            headerTitle = stringResource(R.string.backup_include_header)
                        )
                        if (selection.vaults && hasVaults) {
                            BackupToggleList(
                                items = vaultState.vaults.map {
                                    Triple(it.uuid.toString(), it.name, null)
                                },
                                isSelected = { selection.isVaultSelected(it) },
                                onToggle = ::toggleVault,
                                headerTitle = stringResource(R.string.backup_include_vaults)
                            )
                        }
                        BackupNoticeCard(
                            icon = Icons.Outlined.Shield,
                            text = stringResource(R.string.backup_warning)
                        )
                        BackupPrimaryButton(
                            text = stringResource(R.string.backup_next),
                            enabled = anySelected,
                            onClick = { step = ExportStep.ENCRYPT }
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.backup_encrypt_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.backup_export_password_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = protect, onCheckedChange = { protect = it })
                            Text(
                                text = stringResource(R.string.backup_protect_with_password),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        if (protect) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text(stringResource(R.string.backup_password)) },
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None
                                else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirm,
                                onValueChange = { confirm = it },
                                label = { Text(stringResource(R.string.backup_password_confirm)) },
                                singleLine = true,
                                isError = confirm.isNotEmpty() && confirm != password,
                                visualTransformation = if (showPassword) VisualTransformation.None
                                else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showPassword, onCheckedChange = { showPassword = it })
                                Text(
                                    text = stringResource(R.string.backup_show_password),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        BackupPrimaryButton(
                            text = stringResource(R.string.backup_save_file),
                            enabled = passwordsValid,
                            onClick = { beginExport() }
                        )
                        BackupSecondaryButton(
                            text = stringResource(R.string.backup_back),
                            onClick = { step = ExportStep.SELECT }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    VaultPasswordUnlockSheet(
        state = unlockSheet,
        authType = unlockAuthType,
        subtitle = currentVault?.let {
            stringResource(R.string.backup_unlock_vault_subtitle, it.name)
        },
        onDismiss = {
            // Cancelling a vault's prompt aborts the whole export gate.
            unlockError = null
            vaultQueue = emptyList()
            currentVault = null
        },
        onSubmit = { secret ->
            val vault = currentVault ?: return@VaultPasswordUnlockSheet
            scope.launch {
                when (val result = VaultPasswordManager.verifyPassword(
                    context, vault.uuid, secret
                )) {
                    is VerifyResult.Success -> {
                        unlockError = null
                        unlockSheet.hide()
                        vaultQueue = vaultQueue.drop(1)
                        processVaultQueue()
                    }
                    is VerifyResult.Failed ->
                        unlockError = String.format(wrongPasswordStr, result.attemptsLeft)
                    is VerifyResult.LockedOut ->
                        unlockError = String.format(lockedOutStr, result.cooldownMs / 1000)
                }
            }
        },
        errorMessage = unlockError
    )

    // Selector-gate verification shown when turning the Vaults section on.
    VaultPasswordUnlockSheet(
        state = revealGateSheet,
        authType = revealGateAuthType,
        subtitle = stringResource(R.string.backup_unlock_vaults_gate_subtitle),
        onDismiss = { revealGateError = null },
        onSubmit = { secret ->
            scope.launch {
                when (val result = VaultPasswordManager.verifyPassword(
                    context, VaultPasswordManager.GATE_UUID, secret
                )) {
                    is VerifyResult.Success -> {
                        revealGateError = null
                        revealGateSheet.hide()
                        enableVaults()
                    }
                    is VerifyResult.Failed ->
                        revealGateError = String.format(wrongPasswordStr, result.attemptsLeft)
                    is VerifyResult.LockedOut ->
                        revealGateError = String.format(lockedOutStr, result.cooldownMs / 1000)
                }
            }
        },
        errorMessage = revealGateError
    )
}

// =================================================================================================
// Import wizard
// =================================================================================================

@Composable
fun SettingsBackupImportScreen(navigateUp: () -> Unit) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<BackupRestoreViewModel>()
    val phase by viewModel.importPhase.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    val favoriteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.onImportFilePicked(uri) else navigateUp()
    }

    fun pickFile() {
        importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
    }

    // Auto-open the file picker the first time the screen is shown.
    LaunchedEffect(Unit) {
        if (phase is BackupRestoreViewModel.ImportPhase.PickingFile) pickFile()
    }

    // After a successful import, fire the system favorite-consent dialog for matched local media.
    LaunchedEffect(phase) {
        val done = phase as? BackupRestoreViewModel.ImportPhase.Done ?: return@LaunchedEffect
        val uris = done.result.pendingLocalFavoriteUris
        if (uris.isNotEmpty() && SdkCompat.supportsFavorites) {
            try {
                val intentSender = MediaStore.createFavoriteRequest(
                    context.contentResolver, uris, true
                ).intentSender
                val request = IntentSenderRequest.Builder(intentSender)
                    .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
                    .build()
                favoriteConsentLauncher.launch(request)
            } catch (e: Exception) {
                printError("SettingsBackupImportScreen: failed to request local favorites: ${e.message}")
            }
        }
    }

    BackupWizardScaffold(title = stringResource(R.string.backup_import)) { padding ->
        when (val current = phase) {
            BackupRestoreViewModel.ImportPhase.PickingFile ->
                BackupCenteredMessage(padding) {
                    Text(
                        text = stringResource(R.string.backup_pick_file_prompt),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    BackupPrimaryButton(
                        text = stringResource(R.string.backup_choose_file),
                        onClick = { pickFile() }
                    )
                }

            BackupRestoreViewModel.ImportPhase.Inspecting ->
                BackupCenteredMessage(padding) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.backup_reading_file))
                }

            is BackupRestoreViewModel.ImportPhase.NeedsPassword ->
                ImportPasswordContent(
                    padding = padding,
                    wrongPassword = current.wrongPassword,
                    onSubmit = { viewModel.submitImportPassword(it) }
                )

            is BackupRestoreViewModel.ImportPhase.Configuring ->
                ImportSelectContent(
                    padding = padding,
                    contents = current.contents,
                    selection = selection,
                    onToggle = { section, checked -> viewModel.updateSelection { with(section, checked) } },
                    onToggleVault = { id, checked ->
                        viewModel.updateSelection {
                            val allIds = current.contents.vaults.map { it.uuid }.toSet()
                            val cur = selectedVaultIds ?: allIds
                            copy(selectedVaultIds = if (checked) cur + id else cur - id)
                        }
                    },
                    onRestore = { viewModel.startImport() }
                )

            BackupRestoreViewModel.ImportPhase.Running ->
                BackupProgressContent(padding, selection, progress, stringResource(R.string.backup_restoring))

            is BackupRestoreViewModel.ImportPhase.Done -> {
                val r = current.result
                BackupStatusContent(
                    padding = padding,
                    success = true,
                    title = stringResource(R.string.backup_result_imported),
                    rows = listOf(
                        stringResource(R.string.backup_include_settings) to "${r.settingsRestored}",
                        stringResource(R.string.backup_include_cloud_favorites) to "${r.cloudFavoritesRestored}",
                        stringResource(R.string.backup_include_cloud_configs) to "${r.cloudConfigsRestored}",
                        stringResource(R.string.backup_include_vaults) to
                            stringResource(R.string.backup_vaults_value, r.vaultsRestored, r.vaultMediaRestored)
                    ),
                    note = if (r.pendingLocalFavoriteUris.isNotEmpty()) {
                        stringResource(R.string.backup_import_pending_favorites, r.pendingLocalFavoriteUris.size)
                    } else null,
                    onDone = navigateUp
                )
            }

            is BackupRestoreViewModel.ImportPhase.Failed ->
                BackupStatusContent(
                    padding = padding,
                    success = false,
                    title = stringResource(R.string.backup_error, current.message),
                    rows = emptyList(),
                    note = null,
                    onDone = {
                        viewModel.resetImport()
                        pickFile()
                    },
                    doneLabel = stringResource(R.string.backup_try_again)
                )
        }
    }
}

@Composable
private fun ImportPasswordContent(
    padding: PaddingValues,
    wrongPassword: Boolean,
    onSubmit: (String) -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(wrongPassword) { if (wrongPassword) password = "" }

    BackupScrollColumn(padding) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.backup_import_password_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = stringResource(R.string.backup_import_password_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.backup_password)) },
            singleLine = true,
            isError = wrongPassword,
            supportingText = if (wrongPassword) {
                { Text(stringResource(R.string.backup_wrong_password)) }
            } else null,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showPassword, onCheckedChange = { showPassword = it })
            Text(stringResource(R.string.backup_show_password), modifier = Modifier.padding(start = 4.dp))
        }
        BackupPrimaryButton(
            text = stringResource(android.R.string.ok),
            enabled = password.isNotBlank(),
            onClick = { onSubmit(password) }
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ImportSelectContent(
    padding: PaddingValues,
    contents: BackupContents,
    selection: BackupSelection,
    onToggle: (BackupSection, Boolean) -> Unit,
    onToggleVault: (String, Boolean) -> Unit,
    onRestore: () -> Unit
) {
    val present = ALL_SECTIONS.filter { contents.has(it) }
    val anySelected = present.any { selection.isOn(it) } &&
        (!selection.vaults || present.any { it != BackupSection.VAULTS && selection.isOn(it) } ||
            contents.vaults.any { selection.isVaultSelected(it.uuid) })
    BackupScrollColumn(padding) {
        Spacer(Modifier.height(4.dp))
        if (contents.appVersionName.isNotBlank()) {
            Text(
                text = stringResource(R.string.backup_made_with_version, contents.appVersionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        if (present.isEmpty()) {
            Text(
                text = stringResource(R.string.backup_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            BackupSectionList(
                sections = present,
                isSelected = { selection.isOn(it) },
                onToggle = onToggle,
                headerTitle = stringResource(R.string.backup_restore_header),
                summaryFor = { contents.countLabel(it) }
            )
            if (selection.vaults && contents.vaults.isNotEmpty()) {
                BackupToggleList(
                    items = contents.vaults.map {
                        Triple(
                            it.uuid,
                            it.name,
                            stringResource(R.string.backup_items_count, it.mediaCount)
                        )
                    },
                    isSelected = { selection.isVaultSelected(it) },
                    onToggle = onToggleVault,
                    headerTitle = stringResource(R.string.backup_include_vaults)
                )
            }
        }
        BackupNoticeCard(
            icon = Icons.Outlined.Shield,
            text = stringResource(R.string.backup_warning)
        )
        BackupPrimaryButton(
            text = stringResource(R.string.backup_restore),
            enabled = anySelected,
            onClick = onRestore
        )
        Spacer(Modifier.height(16.dp))
    }
}

// =================================================================================================
// Shared components
// =================================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupWizardScaffold(
    title: String,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
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
        },
        content = content
    )
}

@Composable
internal fun BackupCenteredMessage(
    padding: PaddingValues,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}

/** Maximum content width used to keep the wizard readable on tablets and unfolded foldables. */
private val BACKUP_CONTENT_MAX_WIDTH = 640.dp

/**
 * Scrolling content container shared by the wizard steps. Applies the scaffold insets and a
 * horizontal padding, and constrains the content to an adaptive max width that stays centered
 * on large windows (tablets, unfolded foldables) while filling the width on phones.
 */
@Composable
internal fun BackupScrollColumn(
    padding: PaddingValues,
    verticalSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding()
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = BACKUP_CONTENT_MAX_WIDTH)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content
        )
    }
}

/** Primary wizard action, rendered with the shared [SetupButton] for app-wide consistency. */
@Composable
internal fun BackupPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    SetupButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        applyHorizontalPadding = false,
        applyBottomPadding = false,
        applyInsets = false
    )
}

/** Secondary wizard action (e.g. Back), styled to match [SetupButton] but tonal. */
@Composable
internal fun BackupSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    SetupButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        applyHorizontalPadding = false,
        applyBottomPadding = false,
        applyInsets = false,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

/**
 * Renders a group of [BackupSection] toggles using the shared Settings DSL ([SettingsItem] +
 * [rememberSwitchPreference]) so the cards match the rest of the settings UI. Corner positions
 * are assigned manually since this is laid out in a scrolling Column rather than a LazyColumn.
 */
@Composable
internal fun BackupSectionList(
    sections: List<BackupSection>,
    isSelected: (BackupSection) -> Boolean,
    onToggle: (BackupSection, Boolean) -> Unit,
    headerTitle: String? = null,
    summaryFor: @Composable (BackupSection) -> String = { stringResource(it.summaryRes()) }
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (headerTitle != null) {
            SettingsItem(
                item = SettingsEntity.Header(title = headerTitle),
                applyPaddings = false
            )
        }
        sections.forEachIndexed { index, section ->
            val position = when {
                sections.size == 1 -> Position.Alone
                index == 0 -> Position.Top
                index == sections.lastIndex -> Position.Bottom
                else -> Position.Middle
            }
            val summary = summaryFor(section)
            val pref = rememberSwitchPreference(
                section, isSelected(section), summary, position,
                title = stringResource(section.titleRes()),
                summary = summary,
                isChecked = isSelected(section),
                onCheck = { onToggle(section, it) },
                screenPosition = position
            )
            SettingsItem(item = pref, applyPaddings = false)
        }
    }
}

/**
 * Renders a group of individually-selectable rows (used for picking specific vaults) with the
 * same Settings DSL styling as [BackupSectionList]. [items] are (id, title, summary) triples.
 */
@Composable
internal fun BackupToggleList(
    items: List<Triple<String, String, String?>>,
    isSelected: (String) -> Boolean,
    onToggle: (String, Boolean) -> Unit,
    headerTitle: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsItem(
            item = SettingsEntity.Header(title = headerTitle),
            applyPaddings = false
        )
        items.forEachIndexed { index, (id, title, summary) ->
            val position = when {
                items.size == 1 -> Position.Alone
                index == 0 -> Position.Top
                index == items.lastIndex -> Position.Bottom
                else -> Position.Middle
            }
            val checked = isSelected(id)
            val pref = rememberSwitchPreference(
                id, checked, summary, position,
                title = title,
                summary = summary,
                isChecked = checked,
                onCheck = { onToggle(id, it) },
                screenPosition = position
            )
            SettingsItem(item = pref, applyPaddings = false)
        }
    }
}

@Composable
internal fun BackupProgressContent(
    padding: PaddingValues,
    selection: BackupSelection,
    progress: Map<BackupSection, BackupRestoreViewModel.SectionProgress>,
    header: String
) {
    val sections = ALL_SECTIONS.filter { selection.isOn(it) }
    BackupScrollColumn(padding) {
        Spacer(Modifier.height(4.dp))
        Text(text = header, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        sections.forEach { section ->
            BackupSectionProgressCard(section, progress[section])
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BackupSectionProgressCard(
    section: BackupSection,
    progress: BackupRestoreViewModel.SectionProgress?
) {
    val done = progress?.done == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when {
                done -> Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                else -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(section.titleRes()),
                style = MaterialTheme.typography.titleSmall
            )
            if (!done && progress != null && progress.total > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.current.toFloat() / progress.total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${progress.current} / ${progress.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun BackupStatusContent(
    padding: PaddingValues,
    success: Boolean,
    title: String,
    rows: List<Pair<String, String>>,
    note: String?,
    onDone: () -> Unit,
    doneLabel: String? = null
) {
    // Use a neutral surface with onSurface text for strong, theme-consistent contrast;
    // the success/error state is conveyed by the accent-tinted icon and title only.
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val onContainer = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val icon: ImageVector = if (success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline

    BackupScrollColumn(padding, verticalSpacing = 16.dp) {
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(container)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer
                )
            }
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
                    Text(
                        value,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = onContainer
                    )
                }
            }
            note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = labelColor)
            }
        }
        BackupPrimaryButton(
            text = doneLabel ?: stringResource(R.string.backup_done),
            onClick = onDone
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun BackupNoticeCard(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ----- Section helpers --------------------------------------------------------------------------

internal fun BackupSelection.isOn(section: BackupSection): Boolean = when (section) {
    BackupSection.SETTINGS -> settings
    BackupSection.LOCAL_FAVORITES -> localFavorites
    BackupSection.CLOUD_FAVORITES -> cloudFavorites
    BackupSection.CLOUD_CONFIGS -> cloudConfigs
    BackupSection.VAULTS -> vaults
}

internal fun BackupSelection.with(section: BackupSection, value: Boolean): BackupSelection = when (section) {
    BackupSection.SETTINGS -> copy(settings = value)
    BackupSection.LOCAL_FAVORITES -> copy(localFavorites = value)
    BackupSection.CLOUD_FAVORITES -> copy(cloudFavorites = value)
    BackupSection.CLOUD_CONFIGS -> copy(cloudConfigs = value)
    BackupSection.VAULTS -> copy(vaults = value)
}

internal fun BackupSection.titleRes(): Int = when (this) {
    BackupSection.SETTINGS -> R.string.backup_include_settings
    BackupSection.LOCAL_FAVORITES -> R.string.backup_include_local_favorites
    BackupSection.CLOUD_FAVORITES -> R.string.backup_include_cloud_favorites
    BackupSection.CLOUD_CONFIGS -> R.string.backup_include_cloud_configs
    BackupSection.VAULTS -> R.string.backup_include_vaults
}

internal fun BackupSection.summaryRes(): Int = when (this) {
    BackupSection.SETTINGS -> R.string.backup_include_settings_summary
    BackupSection.LOCAL_FAVORITES -> R.string.backup_include_local_favorites_summary
    BackupSection.CLOUD_FAVORITES -> R.string.backup_include_cloud_favorites_summary
    BackupSection.CLOUD_CONFIGS -> R.string.backup_include_cloud_configs_summary
    BackupSection.VAULTS -> R.string.backup_include_vaults_summary
}

@Composable
internal fun BackupContents.countLabel(section: BackupSection): String = when (section) {
    BackupSection.VAULTS -> stringResource(R.string.backup_vaults_value, vaultCount, vaultMediaCount)
    BackupSection.SETTINGS -> stringResource(R.string.backup_items_count, settingsCount)
    BackupSection.LOCAL_FAVORITES -> stringResource(R.string.backup_items_count, localFavoritesCount)
    BackupSection.CLOUD_FAVORITES -> stringResource(R.string.backup_items_count, cloudFavoritesCount)
    BackupSection.CLOUD_CONFIGS -> stringResource(R.string.backup_items_count, cloudConfigsCount)
}
