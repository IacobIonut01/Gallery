package com.dot.gallery.feature_node.presentation.frameextract

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dot.gallery.core.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface FramePickerEffect {
    data object SelectionLimitReached : FramePickerEffect
    data class ExportFinished(val savedUris: List<Uri>) : FramePickerEffect
}

@HiltViewModel
class FramePickerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val materializer: FrameSourceMaterializer,
    private val workManager: WorkManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow<FramePickerUiState>(FramePickerUiState.PreparingSource(null))
    val state: StateFlow<FramePickerUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<FramePickerEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<FramePickerEffect> = _effects

    private var prepared: PreparedFrameSource? = null
    private var decoder: FrameDecoderSession? = null
    private var previewJob: Job? = null
    private var playbackJob: Job? = null
    private var exportObservationJob: Job? = null
    private var selectedThumbnailJob: Job? = null
    private var previewGeneration = 0L
    private var exportOwnsSource = false
    private val sessionId = savedStateHandle.get<String>(KEY_SESSION_ID)
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: UUID.randomUUID().also { savedStateHandle[KEY_SESSION_ID] = it.toString() }

    private var selection = FrameSelectionReducer.restore(
        savedStateHandle.get<ArrayList<String>>(KEY_SELECTION).orEmpty()
    )
    private var currentIdentity = savedStateHandle.get<String>(KEY_CURRENT_FRAME)
        ?.let(FrameIdentity::decode)
    private var selectedFormat = FrameExportFormat.JPEG
    private var initializedSource: FrameSourceSpec? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            selectedFormat = FrameExportFormat.fromPersisted(Settings.Misc.getFrameExportFormat(context))
            updateReady { it.copy(format = selectedFormat) }
        }
        savedStateHandle.get<String>(KEY_WORK_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let(::observeExport)
    }

    fun initialize(source: FrameSourceSpec) {
        if (savedStateHandle.get<String>(KEY_WORK_ID) != null) return
        if (initializedSource == source && decoder != null) return
        initializedSource = source
        savedStateHandle[KEY_SOURCE] = source
        previewJob?.cancel()
        playbackJob?.cancel()
        decoder?.close()
        prepared?.takeUnless { exportOwnsSource }?.deleteIfOwned()
        prepared = null
        decoder = null
        viewModelScope.launch {
            _state.value = FramePickerUiState.PreparingSource(null)
            try {
                val result = materializer.materialize(source) { progress ->
                    _state.value = FramePickerUiState.PreparingSource(progress)
                }
                prepared = result
                val session = FrameDecoderSession(context, result.sourceUri, result.localFile)
                decoder = session
                val metadata = session.prepare()
                val restored = currentIdentity?.let { session.closest(it.presentationTimeUs) }
                val preferredTime = result.motionPhotoInfo?.presentationTimestampUs
                    ?.takeIf { it >= 0L }
                    ?: source.preferredPresentationTimeUs
                val initial = restored ?: session.resolveInitial(preferredTime, source.initialPositionMs)
                currentIdentity = initial
                savedStateHandle[KEY_CURRENT_FRAME] = initial.encode()
                _state.value = FramePickerUiState.Ready(
                    metadata = metadata,
                    currentFrame = null,
                    selection = selection,
                    selectedThumbnails = emptyList(),
                    filmstrip = emptyList(),
                    isPreviewLoading = true,
                    isPlaying = false,
                    format = selectedFormat,
                    preferredTimeUs = preferredTime,
                )
                loadFrame(initial)
            } catch (error: Throwable) {
                decoder?.close()
                decoder = null
                prepared?.deleteIfOwned()
                prepared = null
                if (error is CancellationException) throw error
                _state.value = FramePickerUiState.Failure(
                    reason = error.message ?: "Source preparation failed",
                    retryable = (error as? FrameSourceException)?.retryable == true,
                )
            }
        }
    }

    fun retry() {
        initializedSource?.let {
            initializedSource = null
            initialize(it)
        }
    }

    fun step(delta: Int) {
        val current = currentIdentity ?: return
        pause()
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            decoder?.step(current, delta)?.let { loadFrame(it) }
        }
    }

    fun togglePlayback() {
        val ready = _state.value as? FramePickerUiState.Ready ?: return
        if (ready.isPlaying) pause() else play()
    }

    fun toggleSelection() {
        val frame = currentIdentity ?: return
        when (val change = FrameSelectionReducer.toggle(selection, frame)) {
            SelectionChange.LimitReached -> _effects.tryEmit(FramePickerEffect.SelectionLimitReached)
            is SelectionChange.Updated -> {
                selection = change.frames
                savedStateHandle[KEY_SELECTION] = ArrayList(selection.map(FrameIdentity::encode))
                updateReady { it.copy(selection = selection) }
                scheduleSelectedThumbnailRefresh()
            }
        }
    }

    fun removeSelection(frame: FrameIdentity) {
        if (frame !in selection) return
        selection = selection.filterNot { it == frame }
        savedStateHandle[KEY_SELECTION] = ArrayList(selection.map(FrameIdentity::encode))
        updateReady { it.copy(selection = selection) }
        scheduleSelectedThumbnailRefresh()
    }

    fun clearSelection() {
        selectedThumbnailJob?.cancel()
        selection = emptyList()
        savedStateHandle[KEY_SELECTION] = arrayListOf<String>()
        updateReady { it.copy(selection = emptyList(), selectedThumbnails = emptyList()) }
    }

    fun jumpTo(frame: FrameIdentity) {
        pause()
        previewJob?.cancel()
        previewJob = viewModelScope.launch { loadFrame(frame) }
    }

    fun chooseFormat(format: FrameExportFormat) {
        selectedFormat = format
        updateReady { it.copy(format = format) }
        viewModelScope.launch(Dispatchers.IO) {
            Settings.Misc.setFrameExportFormat(context, format.persistedValue)
        }
    }

    fun export(includeCurrentIfEmpty: Boolean = false) {
        val source = prepared ?: return
        val frames = if (selection.isEmpty() && includeCurrentIfEmpty) {
            listOfNotNull(currentIdentity)
        } else selection
        if (frames.isEmpty()) return
        val previewToCancel = previewJob
        val playbackToCancel = playbackJob
        val thumbnailsToCancel = selectedThumbnailJob
        val decoderToClose = decoder
        previewJob = null
        playbackJob = null
        selectedThumbnailJob = null
        decoder = null
        _state.value = FramePickerUiState.Exporting(0, frames.size, FrameExportWorker.PHASE_DECODING)
        viewModelScope.launch {
            previewToCancel?.cancelAndJoin()
            playbackToCancel?.cancelAndJoin()
            thumbnailsToCancel?.cancelAndJoin()
            decoderToClose?.closeSafely()
            val workId = FrameExportWorker.enqueue(
                workManager = workManager,
                sessionId = sessionId,
                prepared = source,
                identities = frames,
                format = selectedFormat,
            )
            exportOwnsSource = source.ownership == FrameSourceOwnership.SESSION
            savedStateHandle[KEY_WORK_ID] = workId.toString()
            observeExport(workId)
        }
    }

    fun cancelExport() {
        savedStateHandle.get<String>(KEY_WORK_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let(workManager::cancelWorkById)
    }

    private fun play() {
        val ready = _state.value as? FramePickerUiState.Ready ?: return
        updateReady { it.copy(isPlaying = true) }
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var current = currentIdentity ?: return@launch
            while (isActive) {
                val next = decoder?.step(current, 1) ?: break
                if (next == current) break
                current = next
                loadFrame(next, preservePlaying = true, refreshFilmstrip = false)
                delay(50)
            }
            updateReady { it.copy(isPlaying = false) }
        }
    }

    private fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        updateReady { it.copy(isPlaying = false, isPreviewLoading = false) }
    }

    private suspend fun loadFrame(
        identity: FrameIdentity,
        preservePlaying: Boolean = false,
        refreshFilmstrip: Boolean = true,
    ) {
        val session = decoder ?: return
        val generation = ++previewGeneration
        currentIdentity = identity
        savedStateHandle[KEY_CURRENT_FRAME] = identity.encode()
        updateReady { it.copy(isPreviewLoading = true) }
        try {
            val preview = session.decodePreview(identity, PREVIEW_WIDTH, PREVIEW_HEIGHT)
            val previousFilmstrip = (_state.value as? FramePickerUiState.Ready)?.filmstrip.orEmpty()
            val currentFilmstripIndex = previousFilmstrip.indexOfFirst { it.identity == identity }
            val shouldRefreshFilmstrip = refreshFilmstrip || currentFilmstripIndex < 0 ||
                preservePlaying && currentFilmstripIndex >= previousFilmstrip.lastIndex - 1
            val filmstrip = if (shouldRefreshFilmstrip) {
                session.filmstrip(identity, FILMSTRIP_COUNT).mapNotNull { item ->
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    runCatching {
                        FramePreview(item, session.decodePreview(item, THUMBNAIL_SIZE, THUMBNAIL_SIZE))
                    }.getOrNull()
                }
            } else previousFilmstrip
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (generation != previewGeneration) return
            updateReady {
                it.copy(
                    currentFrame = FramePreview(identity, preview),
                    filmstrip = filmstrip,
                    isPreviewLoading = false,
                    isPlaying = preservePlaying && it.isPlaying,
                )
            }
            if (selection.isNotEmpty() &&
                (_state.value as? FramePickerUiState.Ready)?.selectedThumbnails.isNullOrEmpty()
            ) {
                refreshSelectedThumbnails()
            }
        } catch (error: Throwable) {
            if (generation == previewGeneration) {
                updateReady { it.copy(isPreviewLoading = false) }
            }
            if (error is CancellationException) throw error
            if (!preservePlaying && generation == previewGeneration) {
                _state.value = FramePickerUiState.Failure(error.message ?: "Frame unavailable", true)
            }
        }
    }

    private fun scheduleSelectedThumbnailRefresh() {
        selectedThumbnailJob?.cancel()
        selectedThumbnailJob = viewModelScope.launch { refreshSelectedThumbnails() }
    }

    private suspend fun refreshSelectedThumbnails() {
        val session = decoder ?: return
        val expectedSelection = selection
        val thumbnails = expectedSelection.mapNotNull { identity ->
            runCatching {
                FramePreview(
                    identity,
                    session.decodePreview(identity, THUMBNAIL_SIZE, THUMBNAIL_SIZE),
                )
            }.getOrNull()
        }
        if (selection == expectedSelection) {
            updateReady { it.copy(selectedThumbnails = thumbnails) }
        }
    }

    private fun observeExport(workId: UUID) {
        exportObservationJob?.cancel()
        exportObservationJob = viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workId).filterNotNull().collectLatest { info ->
                val total = info.progress.getInt(FrameExportWorker.KEY_TOTAL, selection.size)
                when (info.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.RUNNING -> {
                        _state.value = FramePickerUiState.Exporting(
                            done = info.progress.getInt(FrameExportWorker.KEY_DONE, 0),
                            total = total,
                            phase = info.progress.getString(FrameExportWorker.KEY_PHASE)
                                ?: FrameExportWorker.PHASE_DECODING,
                        )
                    }
                    WorkInfo.State.SUCCEEDED,
                    WorkInfo.State.FAILED -> {
                        val output = info.outputData
                        val aggregate = FrameExportResultAggregator.aggregate(
                            savedValues = output.getStringArray(FrameExportWorker.KEY_SAVED_URIS).orEmpty().toList(),
                            failed = output.getInt(FrameExportWorker.KEY_FAILED_COUNT, 0),
                            cancelled = false,
                            warnings = output.getInt(FrameExportWorker.KEY_WARNING_COUNT, 0),
                        )
                        val savedUris = aggregate.saved.map(Uri::parse)
                        _state.value = when {
                            savedUris.isEmpty() -> FramePickerUiState.Failure(
                                output.getString(FrameExportWorker.KEY_ERROR) ?: "Frame export failed",
                                true,
                            )
                            aggregate.isPartial -> FramePickerUiState.PartialSuccess(
                                savedUris,
                                aggregate.failed,
                                aggregate.warnings,
                            )
                            else -> FramePickerUiState.Success(savedUris, aggregate.warnings)
                        }
                        savedStateHandle.remove<String>(KEY_WORK_ID)
                        exportOwnsSource = false
                        prepared = null
                        if (savedUris.isNotEmpty()) {
                            _effects.emit(FramePickerEffect.ExportFinished(savedUris))
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        savedStateHandle.remove<String>(KEY_WORK_ID)
                        prepared?.deleteIfOwned()
                        exportOwnsSource = false
                        prepared = null
                        _state.value = FramePickerUiState.Failure("Export cancelled", true)
                    }
                }
            }
        }
    }

    private fun updateReady(transform: (FramePickerUiState.Ready) -> FramePickerUiState.Ready) {
        _state.update { current ->
            if (current is FramePickerUiState.Ready) transform(current) else current
        }
    }

    override fun onCleared() {
        previewJob?.cancel()
        playbackJob?.cancel()
        exportObservationJob?.cancel()
        selectedThumbnailJob?.cancel()
        decoder?.close()
        if (!exportOwnsSource) prepared?.deleteIfOwned()
        super.onCleared()
    }

    companion object {
        private const val KEY_SOURCE = "frame_picker_source"
        private const val KEY_SELECTION = "frame_picker_selection"
        private const val KEY_CURRENT_FRAME = "frame_picker_current"
        private const val KEY_SESSION_ID = "frame_picker_session"
        private const val KEY_WORK_ID = "frame_picker_work_id"
        private const val PREVIEW_WIDTH = 1920
        private const val PREVIEW_HEIGHT = 1920
        private const val THUMBNAIL_SIZE = 256
        private const val FILMSTRIP_COUNT = 24
    }
}
