/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.core.smart.SmartScanScheduler
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.data.data_source.SmartScanDao
import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
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
    private val modelManager: ModelManager,
    private val smartScanScheduler: SmartScanScheduler,
    smartScanDao: SmartScanDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeopleListUiState())
    val uiState: StateFlow<PeopleListUiState> = _uiState.asStateFlow()

    /** Whether an on-device face scan can be started (face detector model installed). */
    val localScanAvailable: Boolean
        get() = modelManager.isReady(ModelGroup.FACE_DETECT) &&
            modelManager.isReady(ModelGroup.FACE_RECOGNITION) &&
            registry.getPeopleProviders().any {
                it.providerType == ProviderType.LOCAL_PEOPLE && it.isAvailable
            }

    private val activeSmartScan = smartScanDao.observeActiveRun()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isScanning: StateFlow<Boolean> = activeSmartScan.map { run ->
        run?.requestedFeatures?.and(SmartScanFeature.PERSONS.bit) != 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val scanProgress: StateFlow<Float> = activeSmartScan.map { run ->
        if (run == null || run.totalMedia <= 0) -1f
        else (run.processedMedia.toFloat() / run.totalMedia.toFloat()) * 100f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1f)

    fun scanForPeople() {
        viewModelScope.launch { smartScanScheduler.manual(SmartScanFeature.PERSONS.bit) }
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
