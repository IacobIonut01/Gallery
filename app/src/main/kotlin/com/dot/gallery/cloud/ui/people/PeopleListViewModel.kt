/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val registry: ProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeopleListUiState())
    val uiState: StateFlow<PeopleListUiState> = _uiState.asStateFlow()

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
