/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.common

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
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.util.DateExt
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getDateExt
import com.dot.gallery.feature_node.presentation.util.getDateHeader
import com.dot.gallery.feature_node.presentation.util.getMonth
import com.dot.gallery.feature_node.presentation.util.update
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        viewModelScope.launch(Dispatchers.IO) {
            handler.toggleFavorite(result, mediaList, favorite)
        }
    }

    fun toggleSelection(index: Int) {
        viewModelScope.launch {
            val item = photoState.value.media[index]
            val isSelected = item.selected || selectedPhotoState.find { it.id == item.id } != null
            photoState.value = photoState.value.copy(
                media = photoState.value.media.apply {
                    get(index).selected = !isSelected
                }
            )
            val selectedPhoto = selectedPhotoState.find { it.id == item.id }
            if (selectedPhoto != null) {
                if (!isSelected) {
                    selectedPhotoState[selectedPhotoState.indexOf(selectedPhoto)] =
                        selectedPhoto.copy(
                            selected = true
                        )
                } else selectedPhotoState.remove(selectedPhoto)
            } else {
                selectedPhotoState.add(item.copy(selected = !isSelected))
            }
            multiSelectState.update(selectedPhotoState.isNotEmpty())
        }
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
        viewModelScope.launch(Dispatchers.IO) {
            flow.onEach { result ->
                val mappedData = ArrayList<MediaItem>()
                val mappedDataWithMonthly = ArrayList<MediaItem>()
                val monthHeaderList = mutableSetOf<String>()
                val data = result.data ?: emptyList()
                if (data.isEmpty()) {
                    return@onEach withContext(Dispatchers.Main) {
                        photoState.update(MediaState())
                    }
                }
                data.groupBy {
                    it.timestamp.getDate(
                        stringToday = "Today"
                        /** Localized in composition */
                        ,
                        stringYesterday = "Yesterday"
                        /** Localized in composition */
                    )
                }.forEach { (date, data) ->
                    val month = getMonth(date)
                    if (month.isNotEmpty() && !monthHeaderList.contains(month)) {
                        monthHeaderList.add(month)
                        mappedDataWithMonthly.add(
                            MediaItem.Header("header_big_$month", month, data)
                        )
                    }
                    val item = MediaItem.Header("header_$date", date, data)
                    mappedData.add(item)
                    mappedDataWithMonthly.add(item)
                    data.forEach { media ->
                        val mediaItem =
                            MediaItem.MediaViewItem.Loaded("media_${media.id}", media)
                        mappedData.add(mediaItem)
                        mappedDataWithMonthly.add(mediaItem)
                    }
                }
                return@onEach withContext(Dispatchers.Main) {
                    if (target != null) {
                        return@withContext photoState.update(
                            MediaState(
                                error = if (result is Resource.Error) result.message
                                    ?: "An error occurred" else "",
                                media = data,
                                mappedMedia = mappedData,
                                mappedMediaWithMonthly = mappedDataWithMonthly
                            )
                        )
                    } else if (albumId != -1L) {
                        val startDate: DateExt = data.last().timestamp.getDateExt()
                        val endDate: DateExt = data.first().timestamp.getDateExt()
                        return@withContext photoState.update(
                            MediaState(
                                error = if (result is Resource.Error) result.message
                                    ?: "An error occurred" else "",
                                media = data,
                                mappedMedia = mappedData,
                                mappedMediaWithMonthly = mappedDataWithMonthly,
                                dateHeader = getDateHeader(startDate, endDate)
                            )
                        )
                    } else {
                        return@withContext photoState.update(
                            MediaState(
                                error = if (result is Resource.Error) result.message
                                    ?: "An error occurred" else "",
                                media = data,
                                mappedMedia = mappedData,
                                mappedMediaWithMonthly = mappedDataWithMonthly
                            )
                        )
                    }
                }
            }.flowOn(Dispatchers.IO).collect()
        }
    }

}