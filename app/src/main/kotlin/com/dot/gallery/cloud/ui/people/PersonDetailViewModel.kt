/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.people

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.presentation.util.mapMediaToItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonDetailUiState(
    val person: PersonInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CloudRepository,
    private val registry: ProviderRegistry
) : ViewModel() {

    private val personId: String = savedStateHandle["personId"] ?: ""

    private val _uiState = MutableStateFlow(PersonDetailUiState())
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaState<Media.UriMedia>())
    val mediaState: StateFlow<MediaState<Media.UriMedia>> = _mediaState.asStateFlow()

    init {
        loadPerson()
    }

    private fun loadPerson() {
        if (personId.isBlank()) return
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            repository.getAllPeople().collect { resource ->
                if (resource is Resource.Success) {
                    val person = resource.data?.find { it.id == personId }
                    _uiState.value = _uiState.value.copy(person = person)
                }
            }
        }

        val providers = registry.getPeopleProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        val provider = providers.first()

        viewModelScope.launch {
            provider.getPersonMedia(personId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val mediaList = resource.data?.filterIsInstance<Media.UriMedia>() ?: emptyList()
                        val mapped = mapMediaToItem(
                            data = mediaList,
                            error = "",
                            albumId = -1L,
                            groupByMonth = false,
                            withMonthHeader = false,
                            groupSimilarMedia = false,
                            defaultDateFormat = Constants.DEFAULT_DATE_FORMAT,
                            extendedDateFormat = Constants.EXTENDED_DATE_FORMAT,
                            weeklyDateFormat = Constants.WEEKLY_DATE_FORMAT
                        )
                        _mediaState.value = mapped
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = resource.message
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        if (personId.isBlank()) return
        val providers = registry.getPeopleProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        val type = providers.first().providerType
        viewModelScope.launch {
            repository.updatePersonName(type, personId, name).onSuccess {
                _uiState.value = _uiState.value.copy(
                    person = _uiState.value.person?.copy(name = name)
                )
            }
        }
    }

    fun updateBirthDate(birthDate: String) {
        if (personId.isBlank()) return
        val providers = registry.getPeopleProviders().filter { it.isAvailable }
        if (providers.isEmpty()) return
        val type = providers.first().providerType
        viewModelScope.launch {
            repository.updatePersonBirthDate(type, personId, birthDate).onSuccess {
                _uiState.value = _uiState.value.copy(
                    person = _uiState.value.person?.copy(birthDate = birthDate)
                )
            }
        }
    }
}
