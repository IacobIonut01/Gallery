package com.dot.gallery.feature_node.presentation.albums

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.AlbumState
import com.dot.gallery.core.Resource
import com.dot.gallery.core.contentFlowObserver
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases,
    contentResolver: ContentResolver,
) : ViewModel() {

    val albumsState = mutableStateOf(AlbumState())

    init {
        viewModelScope.launch {
            getAlbums()
        }
        contentResolver
            .observeUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            .launchIn(viewModelScope)
        contentResolver
            .observeUri(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .launchIn(viewModelScope)
    }

    private suspend fun getAlbums() {
        mediaUseCases.getAlbumsUseCase().onEach { result ->
            when (result) {
                is Resource.Error -> {
                    albumsState.value = AlbumState(
                        error = result.message ?: "An error occurred"
                    )
                }

                is Resource.Loading -> {
                    albumsState.value = AlbumState(
                        isLoading = true
                    )
                }

                is Resource.Success -> {
                    albumsState.value = AlbumState(
                        albums = result.data ?: emptyList()
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun ContentResolver.observeUri(uri: Uri) = contentFlowObserver(uri).map {
        getAlbums()
    }
}