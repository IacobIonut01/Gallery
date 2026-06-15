/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.backup.BackupContents
import com.dot.gallery.core.backup.BackupPasswordException
import com.dot.gallery.core.backup.BackupSection
import com.dot.gallery.core.backup.BackupSelection
import com.dot.gallery.core.backup.ConfigBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupManager: ConfigBackupManager
) : ViewModel() {

    /** Progress of a single [BackupSection]. [total] of 0 means "empty section" (instantly done). */
    data class SectionProgress(val current: Int, val total: Int) {
        val done: Boolean get() = current >= total
    }

    /** Selection of which categories to include in export / apply on import. */
    private val _selection = MutableStateFlow(BackupSelection())
    val selection: StateFlow<BackupSelection> = _selection.asStateFlow()

    fun updateSelection(transform: BackupSelection.() -> BackupSelection) {
        _selection.value = _selection.value.transform()
    }

    /** Per-section progress for the currently running export/import. */
    private val _progress = MutableStateFlow<Map<BackupSection, SectionProgress>>(emptyMap())
    val progress: StateFlow<Map<BackupSection, SectionProgress>> = _progress.asStateFlow()

    // -----------------------------------------------------------------------------------------
    // Export wizard
    // -----------------------------------------------------------------------------------------

    sealed interface ExportPhase {
        /** Picking sections / encryption options. */
        data object Configuring : ExportPhase
        data object Running : ExportPhase
        data class Done(val result: ConfigBackupManager.ExportResult) : ExportPhase
        data class Failed(val message: String) : ExportPhase
    }

    private val _exportPhase = MutableStateFlow<ExportPhase>(ExportPhase.Configuring)
    val exportPhase: StateFlow<ExportPhase> = _exportPhase.asStateFlow()

    fun startExport(destination: Uri, password: String?) {
        if (_exportPhase.value is ExportPhase.Running) return
        viewModelScope.launch {
            _progress.value = emptyMap()
            _exportPhase.value = ExportPhase.Running
            val result = backupManager.exportBackup(
                destination, _selection.value, password
            ) { section, current, total ->
                _progress.update { it + (section to SectionProgress(current, total)) }
            }
            _exportPhase.value = result.fold(
                onSuccess = { ExportPhase.Done(it) },
                onFailure = { ExportPhase.Failed(it.message ?: "Export failed") }
            )
        }
    }

    fun resetExport() {
        _exportPhase.value = ExportPhase.Configuring
        _progress.value = emptyMap()
    }

    // -----------------------------------------------------------------------------------------
    // Import wizard
    // -----------------------------------------------------------------------------------------

    sealed interface ImportPhase {
        /** Waiting for the user to pick a backup file. */
        data object PickingFile : ImportPhase
        /** Reading the backup manifest. */
        data object Inspecting : ImportPhase
        /** The backup is encrypted and needs a password. [wrongPassword] is true after a failed attempt. */
        data class NeedsPassword(val source: Uri, val wrongPassword: Boolean = false) : ImportPhase
        /** Manifest read; user picks what to restore. */
        data class Configuring(
            val source: Uri,
            val password: String?,
            val contents: BackupContents
        ) : ImportPhase
        data object Running : ImportPhase
        data class Done(val result: ConfigBackupManager.ImportResult) : ImportPhase
        data class Failed(val message: String) : ImportPhase
    }

    private val _importPhase = MutableStateFlow<ImportPhase>(ImportPhase.PickingFile)
    val importPhase: StateFlow<ImportPhase> = _importPhase.asStateFlow()

    /** Called once the user has picked a backup file via SAF. */
    fun onImportFilePicked(source: Uri) {
        viewModelScope.launch {
            _importPhase.value = ImportPhase.Inspecting
            if (backupManager.isEncryptedBackup(source)) {
                _importPhase.value = ImportPhase.NeedsPassword(source)
            } else {
                inspect(source, null)
            }
        }
    }

    fun submitImportPassword(password: String) {
        val source = (_importPhase.value as? ImportPhase.NeedsPassword)?.source ?: return
        viewModelScope.launch {
            _importPhase.value = ImportPhase.Inspecting
            inspect(source, password)
        }
    }

    private suspend fun inspect(source: Uri, password: String?) {
        val result = backupManager.inspectBackup(source, password)
        _importPhase.value = result.fold(
            onSuccess = { contents ->
                // Default-select only the sections that actually exist in the backup.
                _selection.value = BackupSelection(
                    settings = contents.has(BackupSection.SETTINGS),
                    localFavorites = contents.has(BackupSection.LOCAL_FAVORITES),
                    cloudFavorites = contents.has(BackupSection.CLOUD_FAVORITES),
                    cloudConfigs = contents.has(BackupSection.CLOUD_CONFIGS),
                    vaults = contents.has(BackupSection.VAULTS)
                )
                ImportPhase.Configuring(source, password, contents)
            },
            onFailure = {
                if (it is BackupPasswordException) {
                    ImportPhase.NeedsPassword(source, wrongPassword = true)
                } else {
                    ImportPhase.Failed(it.message ?: "Import failed")
                }
            }
        )
    }

    fun startImport() {
        val configuring = _importPhase.value as? ImportPhase.Configuring ?: return
        viewModelScope.launch {
            _progress.value = emptyMap()
            _importPhase.value = ImportPhase.Running
            val result = backupManager.importBackup(
                configuring.source, _selection.value, configuring.password
            ) { section, current, total ->
                _progress.update { it + (section to SectionProgress(current, total)) }
            }
            _importPhase.value = result.fold(
                onSuccess = { ImportPhase.Done(it) },
                onFailure = {
                    if (it is BackupPasswordException) {
                        ImportPhase.NeedsPassword(configuring.source, wrongPassword = true)
                    } else {
                        ImportPhase.Failed(it.message ?: "Import failed")
                    }
                }
            )
        }
    }

    fun resetImport() {
        _importPhase.value = ImportPhase.PickingFile
        _progress.value = emptyMap()
    }
}
