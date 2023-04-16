package com.dot.gallery

import android.content.ContentResolver
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.Constants
import com.dot.gallery.core.contentFlowObserver
import com.dot.gallery.feature_node.data.data_source.MediaDatabase
import com.dot.gallery.feature_node.data.data_source.Query
import com.dot.gallery.feature_node.data.data_types.getAlbums
import com.dot.gallery.feature_node.data.data_types.getMedia
import com.dot.gallery.feature_node.data.repository.MediaRepositoryImpl.Companion.URIs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val contentResolver: ContentResolver,
    private val database: MediaDatabase
) : ViewModel() {

    fun observer() {
        contentResolver.contentFlowObserver(URIs).map {
            updateDatabase()
        }.launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun updateDatabase() {
        val bundle = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            putInt(MediaStore.QUERY_ARG_MATCH_FAVORITE, MediaStore.MATCH_INCLUDE)
        }
        viewModelScope.launch {
            val media =
                contentResolver.getMedia(mediaQuery = Query.MediaQuery().copy(bundle = bundle))
            database.getMediaDao().getMedia().mapLatest {
                if (it != media) {
                    Log.d(Constants.TAG, "Media Update")
                    if (it.size > media.size) {
                        val deleted = it.minus(media.toSet())
                        database.getMediaDao().removeMedia(deleted)
                    } else {
                        database.getMediaDao().insertMedia(media)
                    }
                }
            }.launchIn(viewModelScope)
        }
        viewModelScope.launch {
            val albums = contentResolver.getAlbums()
            database.getAlbumDao().getAlbums().mapLatest {
                if (it != albums) {
                    Log.d(Constants.TAG, "Album Update")
                    if (it.size > albums.size) {
                        val deleted = it.minus(albums.toSet())
                        database.getAlbumDao().removeAlbum(deleted)
                    } else {
                        database.getAlbumDao().insertAlbum(albums)
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
}