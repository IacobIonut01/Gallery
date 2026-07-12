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
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.util.mapMediaToItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
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
    private val registry: ProviderRegistry,
    private val blurrer: com.dot.gallery.cloud.local.LocalPeopleBlurrer
) : ViewModel() {

    private val personId: String = savedStateHandle["personId"] ?: ""

    private val _uiState = MutableStateFlow(PersonDetailUiState())
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaState<Media.UriMedia>())
    val mediaState: StateFlow<MediaState<Media.UriMedia>> = _mediaState.asStateFlow()

    /** Non-null (done,total) while a "blur everywhere" batch is running. */
    private val _blurProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val blurProgress: StateFlow<Pair<Int, Int>?> = _blurProgress.asStateFlow()

    /** Other on-device people this person can be merged into. */
    private val _mergeCandidates = MutableStateFlow<List<PersonInfo>>(emptyList())
    val mergeCandidates: StateFlow<List<PersonInfo>> = _mergeCandidates.asStateFlow()

    /** Raw media of this person, used to pick a new cover face. */
    private val _personMedia = MutableStateFlow<List<Media.UriMedia>>(emptyList())
    val personMedia: StateFlow<List<Media.UriMedia>> = _personMedia.asStateFlow()

    fun setCover(media: Media.UriMedia) {
        val id = _uiState.value.person?.id ?: return
        val uri = media.getUri().toString()
        viewModelScope.launch {
            localProvider()?.setCover(id, media.id, uri)
            _uiState.value = _uiState.value.copy(
                person = _uiState.value.person?.copy(thumbnailUrl = uri)
            )
        }
    }

    /** True when the current person is an on-device (local) cluster that supports management. */
    val isLocalPerson: Boolean
        get() = _uiState.value.person?.providerType == com.dot.gallery.cloud.core.ProviderType.LOCAL_PEOPLE

    private fun localProvider(): com.dot.gallery.cloud.local.LocalPeopleProvider? =
        registry.getPeopleProviders()
            .firstOrNull { it.providerType == com.dot.gallery.cloud.core.ProviderType.LOCAL_PEOPLE }
                as? com.dot.gallery.cloud.local.LocalPeopleProvider

    fun hidePerson(onDone: () -> Unit) {
        val id = _uiState.value.person?.id ?: return
        viewModelScope.launch {
            localProvider()?.setHidden(id, true)
            onDone()
        }
    }

    fun mergeInto(targetPersonId: String) {
        val sourceId = _uiState.value.person?.id ?: return
        if (sourceId == targetPersonId) return
        viewModelScope.launch { localProvider()?.mergePeople(sourceId, targetPersonId) }
    }

    fun blurEverywhere(useMosaic: Boolean) {
        val id = _uiState.value.person?.id ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _blurProgress.value = 0 to 0
            blurrer.blurPersonEverywhere(
                personId = id,
                brush = if (useMosaic) com.dot.gallery.feature_node.domain.model.editor.MarkupBrush.Mosaic
                else com.dot.gallery.feature_node.domain.model.editor.MarkupBrush.Blur,
                onProgress = { done, total -> _blurProgress.value = done to total }
            )
            _blurProgress.value = null
        }
    }

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
                    _mergeCandidates.value = resource.data
                        ?.filter {
                            it.id != personId &&
                                it.providerType == com.dot.gallery.cloud.core.ProviderType.LOCAL_PEOPLE
                        } ?: emptyList()
                }
            }
        }

        // Route to the provider that actually owns this person (local vs a specific cloud account),
        // instead of assuming the first available people provider.
        viewModelScope.launch {
            val allPeople = repository.getAllPeople()
                .mapNotNull { (it as? Resource.Success)?.data }
                .first()
            val ownerType = allPeople.find { it.id == personId }?.providerType
            val providers = registry.getPeopleProviders().filter { it.isAvailable }
            if (providers.isEmpty()) return@launch
            val provider = providers.firstOrNull { it.providerType == ownerType }
                ?: providers.first()

            provider.getPersonMedia(personId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val mediaList = resource.data?.filterIsInstance<Media.UriMedia>() ?: emptyList()
                        _personMedia.value = mediaList
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
        val type = _uiState.value.person?.providerType ?: providers.first().providerType
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
