package com.dot.gallery.feature_node.presentation.photos

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ViewModel() {

    private val _photoState = mutableStateOf(PhotosState())
    val photoState: State<PhotosState> = _photoState

    init {
        getMedia()
    }

    private fun getMedia() {
        mediaUseCases.getMediaUseCase().onEach { result ->
            when (result) {
                is Resource.Error -> {
                    _photoState.value = PhotosState(
                        error = result.message ?: "An error occurred"
                    )
                }

                is Resource.Loading -> {
                    _photoState.value = PhotosState(
                        isLoading = true
                    )
                }

                is Resource.Success -> {
                    _photoState.value = PhotosState(
                        media = result.data ?: emptyList()
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}