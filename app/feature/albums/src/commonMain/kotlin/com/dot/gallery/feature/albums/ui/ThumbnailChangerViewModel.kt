package com.dot.gallery.feature.albums.ui

import com.dot.gallery.domain.uri.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.data.repository.CoreMediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject



class ThumbnailChangerViewModel @Inject constructor(
    private val repository: CoreMediaRepository
): ViewModel() {

    var albumId: Long = -1L

    val hasAlbumThumbnail = repository.hasAlbumThumbnail(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val albumDetails = repository.getAlbum(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val albumThumbnail = repository.getAlbumThumbnail(albumId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun updateAlbumThumbnail(uri: Uri) {
        viewModelScope.launch {
            repository.updateAlbumThumbnail(albumId, uri)
        }
    }

}