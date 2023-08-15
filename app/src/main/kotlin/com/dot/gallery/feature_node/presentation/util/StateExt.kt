/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dot.gallery.core.Constants
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun RepeatOnResume(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                action()
            }
        }
    }
}

fun <T> MutableState<T>.update(newState: T) {
    if (value != newState) {
        value = newState
    }
}

fun MediaUseCases.mediaFlowWithType(
    albumId: Long,
    allowedMedia: AllowedMedia
): Flow<Resource<List<Media>>> =
    if (albumId != -1L) {
        getMediaByAlbumWithTypeUseCase(albumId, allowedMedia)
    } else {
        getMediaByTypeUseCase(allowedMedia)
    }

fun MediaUseCases.mediaFlow(albumId: Long, target: String?): Flow<Resource<List<Media>>> =
    if (albumId != -1L) {
        getMediaByAlbumUseCase(albumId)
    } else if (!target.isNullOrEmpty()) {
        when (target) {
            Constants.Target.TARGET_FAVORITES -> getMediaFavoriteUseCase()
            Constants.Target.TARGET_TRASH -> getMediaTrashedUseCase()
            else -> getMediaUseCase()
        }
    } else {
        getMediaUseCase()
    }

fun MutableStateFlow<MediaState>.collectMedia(
    data: List<Media>,
    error: String,
    albumId: Long,
    groupByMonth: Boolean = false,
    withMonthHeader: Boolean = true
) {
    var mappedData: ArrayList<MediaItem>? = ArrayList()
    var mappedDataWithMonthly: ArrayList<MediaItem>? = ArrayList()
    var monthHeaderList: MutableSet<String>? = mutableSetOf()
    /**
     * Allow loading animation if the last state is empty
     */
    if (value.media.isEmpty())
        value = MediaState(isLoading = true)
    data.groupBy {
        if (groupByMonth) {
            it.timestamp.getMonth()
        } else {
            it.timestamp.getDate(
                stringToday = "Today"
                /** Localized in composition */
                ,
                stringYesterday = "Yesterday"
                /** Localized in composition */
            )
        }
    }.forEach { (date, data) ->
        val dateHeader = MediaItem.Header("header_$date", date, data)
        val groupedMedia = data.map {
            MediaItem.MediaViewItem.Loaded("media_${it.id}_${it.label}", it)
        }
        if (groupByMonth) {
            mappedData!!.add(dateHeader)
            mappedDataWithMonthly!!.add(dateHeader)
            mappedData!!.addAll(groupedMedia)
            mappedDataWithMonthly!!.addAll(groupedMedia)
        } else {
            val month = getMonth(date)
            if (month.isNotEmpty() && !monthHeaderList!!.contains(month)) {
                monthHeaderList!!.add(month)
                if (withMonthHeader && mappedDataWithMonthly!!.isNotEmpty()) {
                    mappedDataWithMonthly!!.add(MediaItem.Header("header_big_$month", month, data))
                }
            }
            mappedData!!.add(dateHeader)
            if (withMonthHeader) {
                mappedDataWithMonthly!!.add(dateHeader)
            }
            mappedData!!.addAll(groupedMedia)
            if (withMonthHeader) {
                mappedDataWithMonthly!!.addAll(groupedMedia)
            }
        }
    }
    value = MediaState(
        error = error,
        media = data,
        mappedMedia = mappedData!!,
        mappedMediaWithMonthly = if (withMonthHeader) mappedDataWithMonthly!! else emptyList(),
        dateHeader = data.dateHeader(albumId)
    )
    mappedData = null
    mappedDataWithMonthly = null
    monthHeaderList = null
}

private fun List<Media>.dateHeader(albumId: Long): String =
    if (albumId != -1L) {
        val startDate: DateExt = last().timestamp.getDateExt()
        val endDate: DateExt = first().timestamp.getDateExt()
        getDateHeader(startDate, endDate)
    } else ""