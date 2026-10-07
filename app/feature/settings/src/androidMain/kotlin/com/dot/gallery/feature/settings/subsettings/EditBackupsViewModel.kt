/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.subsettings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.EditBackupManager
import com.dot.gallery.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.dot.gallery.domain.model.UriMedia


class EditBackupsViewModel @Inject constructor(
    private val context: Context,
    private val editBackupManager: EditBackupManager,
    private val editBackupsRuntime: EditBackupsRuntime,
    private val repository: MediaRepository
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val totalSize: Long = 0L,
        val freeSpace: Long = 0L,
        val backups: List<EditBackupManager.BackupInfo> = emptyList(),
        val mediaList: List<UriMedia> = emptyList()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = editBackupManager.getAllBackupInfo()
                .sortedByDescending { it.editTimestamp }
            val size = editBackupManager.getTotalBackupSize()
            val free = context.filesDir.freeSpace

            val backupUris = info.map { it.originalUri }
            val mediaList = if (backupUris.isNotEmpty()) {
                val resource = repository.getMediaListByUris(
                    listOfUris = backupUris,
                    reviewMode = false,
                    onlyMatching = true
                ).first()
                val allMedia = resource.data ?: emptyList()
                val backupMediaIds = info.map { it.mediaId }.toSet()
                allMedia.filter { it.id in backupMediaIds }
            } else emptyList()

            _state.value = State(
                isLoading = false,
                totalSize = size,
                freeSpace = free,
                backups = info,
                mediaList = mediaList
            )
        }
    }

    fun autoCleanup(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = editBackupsRuntime.autoCleanupEditBackups()
            refresh()
            onResult(count)
        }
    }

    fun deleteAll(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            editBackupsRuntime.deleteAllEditBackups()
            refresh()
            onDone()
        }
    }

    fun deleteSelected(mediaIds: List<Long>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            editBackupsRuntime.deleteSelectedEditBackups(mediaIds.toLongArray())
            refresh()
            onDone()
        }
    }
}
