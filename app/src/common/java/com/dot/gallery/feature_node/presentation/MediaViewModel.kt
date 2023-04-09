package com.dot.gallery.feature_node.presentation

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.Constants.Target.TARGET_FAVORITES
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class MediaViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ViewModel() {

    val multiSelectState = mutableStateOf(false)
    var photoState = mutableStateOf(MediaState())
        private set
    val selectedPhotoState = mutableStateListOf<Media>()
    val handler = mediaUseCases.mediaHandleUseCase

    var albumId: Long = -1L
        set(value) {
            getMedia(albumId = value)
            field = value
        }
    var target: String? = null
        set(value) {
            getMedia(target = value)
            field = value
        }

    /**
     * Used in PhotosScreen to retrieve all media
     */
    fun launchInPhotosScreen() {
        getMedia(-1, null)
    }

    fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean = false
    ) {
        viewModelScope.launch {
            handler.toggleFavorite(result, mediaList, favorite)
            photoState.value = photoState.value.copy(
                media = photoState.value.media.minus(mediaList.toSet())
            )
        }
    }

    fun toggleSelection(index: Int) {
        val item = photoState.value.media[index]
        val isSelected = item.selected

        photoState.value = photoState.value.copy(
            media = photoState.value.media.apply {
                get(index).selected = !isSelected
            }
        )
        val selectedPhoto = selectedPhotoState.find { it.id == item.id }
        if (selectedPhoto != null) {
            if (!isSelected) {
                selectedPhotoState[selectedPhotoState.indexOf(selectedPhoto)] = selectedPhoto.copy(
                    selected = true
                )
            } else selectedPhotoState.remove(selectedPhoto)
        } else {
            selectedPhotoState.add(item.copy(selected = !isSelected))
        }
        multiSelectState.value = selectedPhotoState.isNotEmpty()
    }

    private fun getMedia(albumId: Long = -1L, target: String? = null) {
        val flow = if (albumId != -1L) {
            mediaUseCases.getMediaByAlbumUseCase(albumId)
        } else if (!target.isNullOrEmpty()) {
            when (target) {
                TARGET_FAVORITES -> mediaUseCases.getMediaFavoriteUseCase()
                TARGET_TRASH -> mediaUseCases.getMediaTrashedUseCase()
                else -> mediaUseCases.getMediaUseCase()
            }
        } else {
            mediaUseCases.getMediaUseCase()
        }
        flow.onEach { result ->
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
                    /**
                     * Update state only if needed
                     */
                    if (photoState.value.media != result.data) {
                        photoState.value = MediaState(
                            media = result.data ?: emptyList()
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

}