package com.dot.gallery.feature_node.presentation.photos

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.Resource
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.contentFlowObserver
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
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

    init {
        viewModelScope.launch {
            getMedia()
        }
        contentResolver
            .observeUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            .launchIn(viewModelScope)
        contentResolver
            .observeUri(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .launchIn(viewModelScope)
    }

    suspend fun getMedia() {
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

    private fun ContentResolver.observeUri(uri: Uri) = contentFlowObserver(uri).map {
        getMedia()
    }
}