package com.dot.gallery.feature_node.presentation.ignored

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.util.RepeatOnResume
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IgnoredViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
): ViewModel() {

    private val _ignoredState = MutableStateFlow(IgnoredState())
    val blacklistState = _ignoredState.asStateFlow()

    init {
        getIgnoredAlbums()
    }

    @SuppressLint("ComposableNaming")
    @Composable
    fun attachToLifecycle() {
        LaunchedEffect(Unit) {
            getIgnoredAlbums()
        }
    }

    fun removeFromBlacklist(ignoredAlbum: IgnoredAlbum) {
        viewModelScope.launch {
            mediaUseCases.blacklistUseCase.removeFromBlacklist(ignoredAlbum)
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