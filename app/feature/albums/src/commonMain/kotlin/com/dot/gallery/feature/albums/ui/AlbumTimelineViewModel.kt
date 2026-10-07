package com.dot.gallery.feature.albums.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.core.metrics.StartupTracer
import com.dot.gallery.core.restorableAlbumTimelineMediaFlow
import com.dot.gallery.domain.model.MediaState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import com.dot.gallery.domain.model.UriMedia




class AlbumTimelineViewModel constructor(
    @me.tatarka.inject.annotations.Assisted private val savedStateHandle: SavedStateHandle,
    distributor: MediaDistributor,
    trace: (String) -> Unit,
) : ViewModel() {
    @Inject constructor(@me.tatarka.inject.annotations.Assisted savedStateHandle: SavedStateHandle, distributor: MediaDistributor) : this(
        savedStateHandle,
        distributor,
        { label -> StartupTracer.trace(label) {} },
    )
    val albumId: Long = checkNotNull(savedStateHandle["albumId"])
    val mediaState: StateFlow<MediaState<UriMedia>> =
        distributor.restorableAlbumTimelineMediaFlow(albumId)
            .onStart { trace("AlbumSession.start($albumId)") }
            .onCompletion { trace("AlbumSession.stop($albumId)") }
            .stateIn(viewModelScope, SharingStarted.Eagerly, MediaState())
}
