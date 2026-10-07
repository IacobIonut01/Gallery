/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Resource
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.TimelineDateSource
import com.dot.gallery.data.repository.MediaRepository
import com.dot.gallery.domain.util.MediaOrder
import com.dot.gallery.domain.util.MediaGroupType
import com.dot.gallery.domain.model.AllowedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.dot.gallery.domain.util.mapMediaToItem


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

fun MediaRepository.mediaFlowWithType(
    albumId: Long,
    allowedMedia: AllowedMedia
): Flow<Resource<List<UriMedia>>> =
    (if (albumId != -1L) {
        getMediaByAlbumIdWithType(albumId, allowedMedia)
    } else {
        getMediaByType(allowedMedia)
    }).flowOn(Dispatchers.IO).conflate()

fun MediaRepository.mediaFlow(albumId: Long, target: String?): Flow<Resource<List<UriMedia>>> =
    (if (albumId != -1L) {
        getMediaByAlbumId(albumId)
    } else if (!target.isNullOrEmpty()) {
        when (target) {
            Constants.Target.TARGET_FAVORITES -> getFavorites(mediaOrder = MediaOrder.Default)
            Constants.Target.TARGET_TRASH -> getTrashed()
            else -> getMedia()
        }
    } else {
        getMedia()
    })

fun <T : Media> Flow<Resource<List<T>>>.mapMedia(
    albumId: Long,
    groupByMonth: Boolean = false,
    groupByYear: Boolean = false,
    withMonthHeader: Boolean = true,
    groupSimilarMedia: Boolean = false,
    enabledGroupTypes: Set<MediaGroupType> = MediaGroupType.entries.toSet(),
    updateDatabase: () -> Unit,
    defaultDateFormat: String,
    extendedDateFormat: String,
    weeklyDateFormat: String,
    dateSource: TimelineDateSource = TimelineDateSource.CAPTURE_TIME
) = map {
    updateDatabase()
    mapMediaToItem(
        data = it.data ?: emptyList(),
        error = it.message ?: "",
        albumId = albumId,
        groupByMonth = groupByMonth,
        groupByYear = groupByYear,
        withMonthHeader = withMonthHeader,
        groupSimilarMedia = groupSimilarMedia,
        enabledGroupTypes = enabledGroupTypes,
        defaultDateFormat = defaultDateFormat,
        extendedDateFormat = extendedDateFormat,
        weeklyDateFormat = weeklyDateFormat,
        dateSource = dateSource
    )
}

suspend fun <T : Media> MutableStateFlow<MediaState<T>>.collectMedia(
    data: List<T>,
    error: String,
    albumId: Long,
    groupByMonth: Boolean = false,
    groupByYear: Boolean = false,
    withMonthHeader: Boolean = true,
    groupSimilarMedia: Boolean = false,
    enabledGroupTypes: Set<MediaGroupType> = MediaGroupType.entries.toSet(),
    cloudGroupKeyOverrides: Map<Long, String> = emptyMap(),
    defaultDateFormat: String,
    extendedDateFormat: String,
    weeklyDateFormat: String,
    dateSource: TimelineDateSource = TimelineDateSource.CAPTURE_TIME
) = withContext(Dispatchers.IO) {
    emit(
        mapMediaToItem(
            data = data,
            error = error,
            albumId = albumId,
            groupByMonth = groupByMonth,
            groupByYear = groupByYear,
            withMonthHeader = withMonthHeader,
            groupSimilarMedia = groupSimilarMedia,
            enabledGroupTypes = enabledGroupTypes,
            cloudGroupKeyOverrides = cloudGroupKeyOverrides,
            defaultDateFormat = defaultDateFormat,
            extendedDateFormat = extendedDateFormat,
            weeklyDateFormat = weeklyDateFormat,
            dateSource = dateSource
        )
    )
}
