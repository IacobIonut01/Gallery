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
import com.dot.gallery.feature_node.presentation.util.isDate
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
class SearchViewModel @Inject constructor(
    private val mediaUseCases: MediaUseCases
) : ViewModel() {

    var lastQuery = mutableStateOf("")
        private set

    var mediaState = mutableStateOf(MediaState())
        private set

    val selectionState = mutableStateOf(false)
    val selectedMedia = mutableStateListOf<Media>()

    init {
        queryMedia()
    }

    private fun List<Media>.parseQuery(query: String): List<Media> {
        if (query.isEmpty())
            return emptyList()
        return filter { item ->
            if (query.isDate()) {
                return@filter item.toString().contains(query, true)
            }
            val queries = query.split("\\s".toRegex())
            var found = false
            queries.forEach {
                found = item.toString().contains(it, true)
            }
            return@filter found
        }
    }

    fun queryMedia(query: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                lastQuery.value = query
            }
            mediaUseCases.getMediaUseCase().onEach { result ->
                val mappedData = ArrayList<MediaItem>()
                val monthHeaderList = ArrayList<String>()
                var data = result.data ?: emptyList()
                if (data.isEmpty()) {
                    return@onEach withContext(Dispatchers.Main) {
                        mediaState.value = MediaState()
                    }
                }
                data = data.parseQuery(query)
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
                    }
                    val item = MediaItem.Header("header_$date", date, data)
                    mappedData.add(item)
                    for (media in data) {
                        val mediaItem =
                            MediaItem.MediaViewItem.Loaded("media_${media.id}", media)
                        mappedData.add(mediaItem)
                    }
                }
                withContext(Dispatchers.Main) {
                    mediaState.update(
                        MediaState(
                            error = if (result is Resource.Error) result.message
                                ?: "An error occurred" else "",
                            media = data,
                            mappedMedia = mappedData
                        )
                    )
                }
            }.flowOn(Dispatchers.IO).collect()
        }
    }

}