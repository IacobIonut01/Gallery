package com.dot.gallery.feature_node.presentation.photos

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases,
    contentResolver: ContentResolver,
) : ViewModel() {

    val photoState = mutableStateOf(PhotosState())

    init {
        getMedia()
        contentResolver
            .result(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            .launchIn(viewModelScope)
        contentResolver
            .result(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .launchIn(viewModelScope)
    }

    private fun getMedia() {
        mediaUseCases.getMediaUseCase().onEach { result ->
            when (result) {
                is Resource.Error -> {
                    photoState.value = PhotosState(
                        error = result.message ?: "An error occurred"
                    )
                }

                is Resource.Loading -> {
                    photoState.value = PhotosState(
                        isLoading = true
                    )
                }

                is Resource.Success -> {
                    photoState.value = PhotosState(
                        media = result.data ?: emptyList()
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun ContentResolver.result(uri: Uri) = observe(uri).map {
        getMedia()
    }

    /**
     * Register an observer class that gets callbacks when data identified by a given content URI
     * changes.
     */
    private fun ContentResolver.observe(uri: Uri) = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                Log.d("Media", "Media changed")
                trySend(true)
            }

            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }
        }
        registerContentObserver(uri, true, observer)
        // trigger first.
        trySend(false)
        awaitClose {
            unregisterContentObserver(observer)
        }
    }
}