/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.dot.gallery.core.ml.DownloadInfo
import com.dot.gallery.core.ml.ModelFileInfo
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.core.ml.ModelStatus
import com.dot.gallery.core.workers.cancelModelDownload
import com.dot.gallery.core.workers.downloadModels
import com.dot.gallery.core.workers.forceMetadataCollect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartFeaturesViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val workManager: WorkManager
) : ViewModel() {

    // Per-group observable state. UI screens pass the relevant [ModelGroup] (SEARCH for smart
    // search + categories, CUTOUT for subject cutout) so each feature is managed independently.
    fun modelStatus(group: ModelGroup): StateFlow<ModelStatus> = modelManager.status(group)
    fun downloadProgress(group: ModelGroup): StateFlow<Float> = modelManager.downloadProgress(group)
    fun errorMessage(group: ModelGroup): StateFlow<String?> = modelManager.errorMessage(group)
    fun downloadInfo(group: ModelGroup): StateFlow<DownloadInfo> = modelManager.downloadInfo(group)

    fun installedSize(group: ModelGroup): Long = modelManager.getInstalledSize(group)

    fun getFileInfos(group: ModelGroup): List<ModelFileInfo> = modelManager.getFileInfos(group)

    val hasInternetPermission: Boolean get() = modelManager.hasInternetPermission
    val areAiFeaturesAvailable: Boolean get() = modelManager.areAiFeaturesAvailable

    val isMetadataWorkerRunning: StateFlow<Boolean> = workManager.getWorkInfosFlow(
        WorkQuery.fromUniqueWorkNames("MetadataCollection")
    ).map { infos ->
        infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val metadataProgress: StateFlow<Int> = workManager.getWorkInfosFlow(
        WorkQuery.fromUniqueWorkNames("MetadataCollection")
    ).map { infos ->
        infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            ?.progress?.getInt("progress", -1) ?: -1
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = -1
    )

    fun downloadModels(group: ModelGroup) {
        if (!modelManager.hasInternetPermission) return
        workManager.downloadModels(group)
    }

    fun cancelDownload(group: ModelGroup) {
        workManager.cancelModelDownload(group)
        viewModelScope.launch {
            modelManager.deleteModels(group)
        }
    }

    fun deleteModels(group: ModelGroup) {
        viewModelScope.launch {
            modelManager.deleteModels(group)
        }
    }

    fun refreshMetadata() {
        workManager.forceMetadataCollect()
    }
}
