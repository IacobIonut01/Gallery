package com.dot.gallery.feature_node.presentation.ignored.setup

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.ignored.IgnoredState
import com.dot.gallery.feature_node.presentation.util.RepeatOnResume
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IgnoredSetupViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
): ViewModel() {

    private val _uiState = MutableStateFlow(IgnoredSetupState())
    val uiState = _uiState.asStateFlow()

    var isLabelError by mutableStateOf(false)

    private val _ignoredState = MutableStateFlow(IgnoredState())
    val blacklistState = _ignoredState.asStateFlow()

    init {
        getIgnoredAlbums()
    }

    @SuppressLint("ComposableNaming")
    @Composable
    fun attachToLifecycle() {
        RepeatOnResume {
            getIgnoredAlbums()
        }
    }

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
            mediaUseCases.blacklistUseCase.addToBlacklist(ignoredAlbum)
        }
    }

    private fun getIgnoredAlbums() {
        viewModelScope.launch {
            mediaUseCases.blacklistUseCase.blacklistedAlbums.collectLatest {
                _ignoredState.emit(IgnoredState(it))
            }
        }
    }
}