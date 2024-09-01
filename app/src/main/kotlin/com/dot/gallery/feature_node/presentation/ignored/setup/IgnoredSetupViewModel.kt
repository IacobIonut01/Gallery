package com.dot.gallery.feature_node.presentation.ignored.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.ignored.IgnoredState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IgnoredSetupViewModel @Inject constructor(
    private val repository: MediaRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(IgnoredSetupState())
    val uiState = _uiState.asStateFlow()

    var isLabelError by mutableStateOf(false)

    val blacklistState = repository.getBlacklistedAlbums()
        .map { IgnoredState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), IgnoredState())

    fun setLabel(label: String) {
        _uiState.value = _uiState.value.copy(label = label)
        isLabelError = label.isEmpty()
    }

    fun setLocation(location: Int) {
        _uiState.value = _uiState.value.copy(location = location)
    }

    fun setType(type: IgnoredType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun setMatchedAlbums(matchedAlbums: List<Album>) {
        _uiState.value = _uiState.value.copy(matchedAlbums = matchedAlbums)
    }

    fun reset() {
        _uiState.value = IgnoredSetupState()
    }

    fun addToIgnored(ignoredAlbum: IgnoredAlbum) {
        viewModelScope.launch {
            repository.addBlacklistedAlbum(ignoredAlbum)
        }
    }

}