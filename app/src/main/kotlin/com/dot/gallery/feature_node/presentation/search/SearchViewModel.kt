/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.search

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.xdrop.fuzzywuzzy.FuzzySearch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ViewModel() {

    var lastQuery = mutableStateOf("")
        private set

    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState = _mediaState.asStateFlow()

    val selectionState = mutableStateOf(false)
    val selectedMedia = mutableStateListOf<Media>()

    init {
        queryMedia()
    }

    private suspend fun List<Media>.parseQuery(query: String): List<Media> {
        return withContext(Dispatchers.IO) {
            if (query.isEmpty())
                return@withContext emptyList()
            val matches = FuzzySearch.extractSorted(query, this@parseQuery, { it.toString() }, 60)
            return@withContext matches.map { it.referent }.ifEmpty { emptyList() }
        }
    }

    fun clearQuery() = queryMedia("")

    fun queryMedia(query: String = "") {
        viewModelScope.launch {
            lastQuery.value = query
            mediaUseCases.getMediaUseCase().flowOn(Dispatchers.IO).collectLatest { result ->
                val mappedData = ArrayList<MediaItem>()
                val monthHeaderList = ArrayList<String>()
                val data = result.data ?: emptyList()
                if (data == mediaState.value.media) return@collectLatest
                val error = if (result is Resource.Error) result.message
                    ?: "An error occurred" else ""
                if (data.isEmpty()) {
                    return@collectLatest _mediaState.emit(MediaState())
                }
                _mediaState.value = MediaState(isLoading = true)
                val parsedData = data.parseQuery(query)
                parsedData.groupBy {
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
                    }
                    mappedData.add(MediaItem.Header("header_$date", date, data))
                    mappedData.addAll(data.map {
                        MediaItem.MediaViewItem.Loaded(
                            "media_${it.id}_${it.label}",
                            it
                        )
                    })
                }
                _mediaState.value =
                    MediaState(
                        error = error,
                        media = parsedData,
                        mappedMedia = mappedData
                    )
            }
        }
    }

}