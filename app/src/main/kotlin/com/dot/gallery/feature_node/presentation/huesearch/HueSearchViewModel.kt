package com.dot.gallery.feature_node.presentation.huesearch

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dot.gallery.core.Constants
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.core.MediaHandler
import com.dot.gallery.core.Settings
import com.dot.gallery.core.workers.startIndexingByHue
import com.dot.gallery.core.workers.stopIndexingByHue
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaMetadataState
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.util.add
import com.dot.gallery.feature_node.presentation.util.mapMediaToItem
import com.dot.gallery.feature_node.presentation.util.remove
import com.dot.gallery.feature_node.presentation.util.update
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HueSearchViewModel @Inject constructor(
    mediaDistributor: MediaDistributor,
    private val mediaHandler: MediaHandler,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _hueState = MutableStateFlow(Color.Cyan)
    val hueState = _hueState.asStateFlow()

    val isRunning by lazy { workManager.getWorkInfosByTagFlow("HueIndexer")
        .map { it.lastOrNull()?.state == WorkInfo.State.RUNNING }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false) }

    val progress by lazy { workManager.getWorkInfosByTagFlow("HueIndexer")
        .map { it.lastOrNull()?.progress?.getFloat("progress", 0f) ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f) }

    val indexedImageCount = mediaHandler.getHueIndexedMediaCount()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            0
        )

    val imagesByHue = mediaDistributor.hueSearchMediaFlow(hueState)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            MediaState()
        )

    fun setColor(color: Color) {
        _hueState.value = color
    }

    fun startIndexing() {
        workManager.startIndexingByHue()
    }

    fun stopIndexing() {
        workManager.stopIndexingByHue()
    }

    fun deleteHueIndexData() {
        viewModelScope.launch(Dispatchers.IO) {
            mediaHandler.deleteHueIndexData()
        }
    }
}