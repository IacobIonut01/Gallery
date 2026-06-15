/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dot.gallery.cloud.sync.CloudUploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadItemProgress(
    val fileName: String,
    val albumName: String,
    val progress: Float, // 0..1, -1 = pending
    val status: UploadItemStatus = UploadItemStatus.PENDING
)

enum class UploadItemStatus {
    PENDING, UPLOADING, COMPLETED, FAILED
}

data class UploadDetailsUiState(
    val isWorkerRunning: Boolean = false,
    val currentFileName: String = "",
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val failedItems: Int = 0,
    val items: List<UploadItemProgress> = emptyList()
)

@HiltViewModel
class UploadDetailsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadDetailsUiState())
    val uiState: StateFlow<UploadDetailsUiState> = _uiState.asStateFlow()

    init {
        observeWorkProgress()
    }

    private fun observeWorkProgress() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(CloudUploadWorker.WORK_NAME_ONCE)
                .collect { workInfos ->
                    val info = workInfos.firstOrNull()
                    if (info == null) {
                        _uiState.value = _uiState.value.copy(isWorkerRunning = false)
                        return@collect
                    }

                    val running = info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED

                    val progress = info.progress
                    val currentFile = progress.getString(CloudUploadWorker.KEY_CURRENT_FILE) ?: ""
                    val totalItems = progress.getInt(CloudUploadWorker.KEY_TOTAL_ITEMS, 0)
                    val completedItems = progress.getInt(CloudUploadWorker.KEY_COMPLETED_ITEMS, 0)
                    val failedItems = progress.getInt(CloudUploadWorker.KEY_FAILED_ITEMS, 0)

                    // Build items list from progress data
                    val completedNames = progress.getStringArray(CloudUploadWorker.KEY_COMPLETED_FILES) ?: emptyArray()
                    val failedNames = progress.getStringArray(CloudUploadWorker.KEY_FAILED_FILES) ?: emptyArray()

                    val items = mutableListOf<UploadItemProgress>()
                    // Currently uploading
                    if (currentFile.isNotEmpty()) {
                        items.add(
                            UploadItemProgress(
                                fileName = currentFile,
                                albumName = "",
                                progress = -1f,
                                status = UploadItemStatus.UPLOADING
                            )
                        )
                    }
                    // Completed items
                    completedNames.forEach { name ->
                        items.add(
                            UploadItemProgress(
                                fileName = name,
                                albumName = "",
                                progress = 1f,
                                status = UploadItemStatus.COMPLETED
                            )
                        )
                    }
                    // Failed items
                    failedNames.forEach { name ->
                        items.add(
                            UploadItemProgress(
                                fileName = name,
                                albumName = "",
                                progress = 0f,
                                status = UploadItemStatus.FAILED
                            )
                        )
                    }

                    _uiState.value = UploadDetailsUiState(
                        isWorkerRunning = running,
                        currentFileName = currentFile,
                        totalItems = totalItems,
                        completedItems = completedItems,
                        failedItems = failedItems,
                        items = items
                    )
                }
        }
    }
}
