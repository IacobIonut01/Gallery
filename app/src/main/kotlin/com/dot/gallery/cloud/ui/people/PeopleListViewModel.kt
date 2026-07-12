/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import com.dot.gallery.core.workers.FaceIndexerWorker
import com.dot.gallery.core.workers.forceFaceIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PeopleListUiState(
    val people: List<PersonInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PeopleListViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val registry: ProviderRegistry,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeopleListUiState())
    val uiState: StateFlow<PeopleListUiState> = _uiState.asStateFlow()

    /** Whether an on-device face scan can be started (face detector model installed). */
    val localScanAvailable: Boolean
        get() = registry.getPeopleProviders().any {
            it.providerType == ProviderType.LOCAL_PEOPLE && it.isAvailable
        }

    private val faceIndexInfos = workManager.getWorkInfosFlow(
        WorkQuery.fromUniqueWorkNames(FaceIndexerWorker.WORK_NAME)
    )

    /** True while an on-device face indexing pass is running or enqueued. */
    val isScanning: StateFlow<Boolean> = faceIndexInfos.map { infos ->
        infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Scan progress in 0..100, or -1 when indeterminate / idle. */
    val scanProgress: StateFlow<Float> = faceIndexInfos.map { infos ->
        infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            ?.progress?.getFloat(FaceIndexerWorker.KEY_PROGRESS, -1f) ?: -1f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1f)

    /** Manually (re)start on-device face indexing. */
    fun scanForPeople() {
        workManager.forceFaceIndex()
    }

    init {
        loadPeople()
        viewModelScope.launch {
            repository.peopleInvalidation.collect {
                loadPeople()
            }
        }
    }

    fun loadPeople() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            repository.getAllPeople().collect { resource ->
                when (resource) {
                    is Resource.Success -> _uiState.value = PeopleListUiState(
                        people = resource.data ?: emptyList()
                    )
                    is Resource.Error -> _uiState.value = PeopleListUiState(
                        error = resource.message
                    )
                }
            }
        }
    }
}
