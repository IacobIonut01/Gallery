package com.dot.gallery.feature_node.presentation.huesearch

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Settings
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
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
    private val repository: MediaRepository,
    val handler: MediaHandleUseCase,
    private val workManager: WorkManager,
) : ViewModel() {

    var initHue: Long = Color.Cyan.toArgb().toLong()
    var updateDatabase: () -> Unit = {}

    val _hueState = MutableStateFlow(Color(initHue))
    val hueState = _hueState.asStateFlow()

    private val defaultDateFormat =
        repository.getSetting(Settings.Misc.DEFAULT_DATE_FORMAT, Constants.DEFAULT_DATE_FORMAT)
            .stateIn(viewModelScope, SharingStarted.Eagerly, Constants.DEFAULT_DATE_FORMAT)

    private val extendedDateFormat =
        repository.getSetting(Settings.Misc.EXTENDED_DATE_FORMAT, Constants.EXTENDED_DATE_FORMAT)
            .stateIn(viewModelScope, SharingStarted.Eagerly, Constants.EXTENDED_DATE_FORMAT)

    private val weeklyDateFormat =
        repository.getSetting(Settings.Misc.WEEKLY_DATE_FORMAT, Constants.WEEKLY_DATE_FORMAT)
            .stateIn(viewModelScope, SharingStarted.Eagerly, Constants.WEEKLY_DATE_FORMAT)

    val classifiedImageCount = repository.getHueClassifiedImageCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val isRunning = workManager.getWorkInfosByTagFlow("HueClassifier")
        .map { it.lastOrNull()?.state == WorkInfo.State.RUNNING }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val progress = workManager.getWorkInfosByTagFlow("HueClassifier")
        .map { it.lastOrNull()?.progress?.getFloat("progress", 0f) ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0f)

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaState by lazy {
        combine(
            repository.getNearestImagesByHueFlow(_hueState),
            combine(
                defaultDateFormat,
                extendedDateFormat,
                weeklyDateFormat
            ) { defaultDateFormat, extendedDateFormat, weeklyDateFormat ->
                Triple(defaultDateFormat, extendedDateFormat, weeklyDateFormat)
            }
        ) { data, (defaultDateFormat, extendedDateFormat, weeklyDateFormat) ->
            updateDatabase.invoke()
            mapMediaToItem(
                data = data,
                error = "",
                albumId = -1,
                defaultDateFormat = defaultDateFormat,
                extendedDateFormat = extendedDateFormat,
                weeklyDateFormat = weeklyDateFormat
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), MediaState())
    }

    val selectionState = mutableStateOf(false)
    val selectedMedia = mutableStateOf<Set<Long>>(emptySet())

    fun toggleSelection(mediaState: MediaState<Media.HueClassifiedMedia>, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = mediaState.media[index]
            val selectedPhoto = selectedMedia.value.find { it == item.id }
            if (selectedPhoto != null) {
                selectedMedia.remove(selectedPhoto)
            } else {
                selectedMedia.add(item.id)
            }
            selectionState.update(selectedMedia.value.isNotEmpty())
        }
    }

    fun setColor(color: Color) {
        _hueState.value = color
    }

    fun startClassification() {
        workManager.startHueClassification()
    }

    fun stopClassification() {
        workManager.stopHueClassification()
    }

    fun deleteClassifications() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHueClassifications()
        }
    }

    fun <T : Media> addMedia(vault: Vault, media: T) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addMedia(vault, media)
        }
    }
}