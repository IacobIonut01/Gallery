package com.dot.gallery.feature_node.presentation.photos

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.core.contentFlowObserver
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.util.getDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases,
    contentResolver: ContentResolver,
) : ViewModel() {

    val photoState = mutableStateOf(MediaState())

    var albumId: Long = -1L
        set(value) {
            viewModelScope.launch {
                getMedia(value)
            }
            field = value
        }

    init {
        viewModelScope.launch {
            getMedia(albumId)
        }
        contentResolver
            .observeUri(
                arrayOf(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                )
            ).launchIn(viewModelScope)
    }

    suspend fun getMedia(albumId: Long = -1L) {
        if (albumId != -1L) {
            mediaUseCases.getMediaByAlbumUseCase(albumId).onEach { result ->
                when (result) {
                    is Resource.Error -> {
                        photoState.value = MediaState(
                            error = result.message ?: "An error occurred"
                        )
                    }

                    is Resource.Loading -> {
                        photoState.value = MediaState(
                            isLoading = true
                        )
                    }

                    is Resource.Success -> {
                        photoState.value = MediaState(
                            media = result.data ?: emptyList()
                        )
                    }

                }
            }.launchIn(viewModelScope)
        } else {
            mediaUseCases.getMediaUseCase().onEach { result ->
                when (result) {
                    is Resource.Error -> {
                        photoState.value = MediaState(
                            error = result.message ?: "An error occurred"
                        )
                    }

                    is Resource.Loading -> {
                        photoState.value = MediaState(
                            isLoading = true
                        )
                    }

                    is Resource.Success -> {
                        photoState.value = MediaState(
                            media = result.data ?: emptyList()
                        )
                    }
                }
            }.launchIn(viewModelScope)
        }
    }

    private fun ContentResolver.observeUri(uri: Array<Uri>) = contentFlowObserver(uri).map {
        getMedia(albumId)
    }
}