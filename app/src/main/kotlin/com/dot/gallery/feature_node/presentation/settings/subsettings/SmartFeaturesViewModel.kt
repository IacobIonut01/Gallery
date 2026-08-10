/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings.subsettings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.dot.gallery.core.Settings
import com.dot.gallery.core.ml.DownloadInfo
import com.dot.gallery.core.ml.ModelFileInfo
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.core.ml.ModelStatus
import com.dot.gallery.core.workers.cancelModelDownload
import com.dot.gallery.core.workers.downloadModels
import com.dot.gallery.core.smart.SmartScanPlan
import com.dot.gallery.core.smart.SmartScanScheduler
import com.dot.gallery.feature_node.data.data_source.SmartScanDao
import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
import com.dot.gallery.feature_node.data.data_source.SmartScanPhaseEntity
import com.dot.gallery.feature_node.data.data_source.SmartScanRunEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartFeaturesViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val workManager: WorkManager,
    private val smartScanScheduler: SmartScanScheduler,
    smartScanDao: SmartScanDao,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    // Per-group observable state. UI screens pass the relevant [ModelGroup] (SEARCH for smart
    // search + categories, CUTOUT for subject cutout) so each feature is managed independently.
    fun modelStatus(group: ModelGroup): StateFlow<ModelStatus> = modelManager.status(group)
    fun downloadProgress(group: ModelGroup): StateFlow<Float> = modelManager.downloadProgress(group)
    fun errorMessage(group: ModelGroup): StateFlow<String?> = modelManager.errorMessage(group)
    fun downloadInfo(group: ModelGroup): StateFlow<DownloadInfo> = modelManager.downloadInfo(group)

    fun installedSize(group: ModelGroup): Long = modelManager.getInstalledSize(group)

    suspend fun getFileInfos(group: ModelGroup): List<ModelFileInfo> = modelManager.getFileInfos(group)

    val hasInternetPermission: Boolean get() = modelManager.hasInternetPermission
    val areAiFeaturesAvailable: Boolean get() = modelManager.areAiFeaturesAvailable

    val includeIgnoredAlbums: StateFlow<Boolean> = Settings.SmartFeatures.includeIgnoredAlbums(context).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val activeSmartScan: StateFlow<SmartScanRunEntity?> = smartScanDao.observeActiveRun().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val latestSmartScan: StateFlow<SmartScanRunEntity?> = smartScanDao.observeLatestRun().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSmartScanPhases: StateFlow<List<SmartScanPhaseEntity>> = activeSmartScan
        .flatMapLatest { run ->
            if (run == null) flowOf(emptyList()) else smartScanDao.observePhases(run.runId)
        }
        .map { phases ->
            phases.sortedBy { SmartScanPlan.orderedPhases.indexOf(it.phase) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

    fun setIncludeIgnoredAlbums(include: Boolean) {
        viewModelScope.launch {
            Settings.SmartFeatures.setIncludeIgnoredAlbums(context, include)
            smartScanScheduler.fullRefresh()
        }
    }

    fun refreshMetadata() = request(SmartScanFeature.METADATA.bit)

    fun refreshEmbeddings() = request(SmartScanFeature.EMBEDDINGS.bit)

    fun refreshCategories() = request(SmartScanFeature.CATEGORIES.bit)

    fun refreshPersons() = request(SmartScanFeature.PERSONS.bit)

    fun refreshAll() {
        viewModelScope.launch { smartScanScheduler.all(userVisible = true) }
    }

    fun fullRefresh() {
        viewModelScope.launch { smartScanScheduler.fullRefresh() }
    }

    fun cancelActiveScan() {
        val runId = activeSmartScan.value?.runId ?: return
        viewModelScope.launch { smartScanScheduler.cancel(runId) }
    }

    fun retryLatestScan() {
        viewModelScope.launch { smartScanScheduler.retryFailed(latestSmartScan.value?.runId) }
    }

    private fun request(features: Int) {
        viewModelScope.launch { smartScanScheduler.manual(features) }
    }
}
