/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.presentation.picker.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.presentation.components.StickyHeader
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.presentation.common.components.MediaComponent
import com.dot.gallery.feature_node.presentation.util.add
import com.dot.gallery.feature_node.presentation.util.remove
import com.dot.gallery.feature_node.presentation.util.size
import com.dot.gallery.feature_node.presentation.util.vibrate
import com.dot.gallery.ui.theme.Dimens
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun PickerMediaScreen(
    mediaState: StateFlow<MediaState>,
    selectedMedia: MutableState<Set<Long>>,
    allowSelection: Boolean,
) {
    val scope = rememberCoroutineScope()
    val stringToday = stringResource(id = R.string.header_today)
    val stringYesterday = stringResource(id = R.string.header_yesterday)
    val state by mediaState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val isCheckVisible = rememberSaveable { mutableStateOf(allowSelection) }

    /** Glide Preloading **/
    val preloadingData = rememberGlidePreloadingData(
        data = state.media,
        preloadImageSize = Size(50f, 50f)
    ) { media: Media, requestBuilder: RequestBuilder<Drawable> ->
        requestBuilder
            .signature(MediaStoreSignature(media.mimeType, media.timestamp, media.orientation))
            .load(media.uri)
    }
    /** ************ **/
    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(Dimens.Photo()),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        items(
            items = state.mappedMedia,
            key = { if (it is MediaItem.MediaViewItem) it.media.toString() else it.key },
            contentType = { it.key.startsWith("media_") },
            span = { item ->
                GridItemSpan(if (item.key.isHeaderKey) maxLineSpan else 1)
            }
        ) { item ->
            when (item) {
                is MediaItem.Header -> {
                    val isChecked = rememberSaveable { mutableStateOf(false) }
                    if (allowSelection) {
                        LaunchedEffect(selectedMedia.value.size) {
                            // Partial check of media items should not check the header
                            isChecked.value = selectedMedia.value.containsAll(item.data.map { it.id }.toSet())
                        }
                    }
                    val title = item.text
                        .replace("Today", stringToday)
                        .replace("Yesterday", stringYesterday)
                    val view = LocalView.current
                    StickyHeader(
                        date = title,
                        showAsBig = item.key.contains("big"),
                        isCheckVisible = isCheckVisible,
                        isChecked = isChecked
                    ) {
                        if (allowSelection) {
                            view.vibrate()
                            scope.launch {
                                isChecked.value = !isChecked.value
                                val list = item.data.map { it.id }
                                if (isChecked.value) {
                                    selectedMedia.add(list)
                                } else selectedMedia.remove(list)
                            }
                        }
                    }
                }

                is MediaItem.MediaViewItem -> {
                    val mediaIndex = state.media.indexOf(item.media).coerceAtLeast(0)
                    val (media, preloadRequestBuilder) = preloadingData[mediaIndex]
                    val view = LocalView.current
                    val selectionState = remember { mutableStateOf(true) }
                    MediaComponent(
                        media = media,
                        selectionState = selectionState,
                        selectedMedia = selectedMedia,
                        preloadRequestBuilder = preloadRequestBuilder,
                        onItemLongClick = {
                            view.vibrate()
                            val id = media.id
                            if (allowSelection) {
                                if (selectedMedia.value.contains(id)) selectedMedia.remove(id)
                                else selectedMedia.add(id)
                            } else if (!selectedMedia.value.contains(id) && selectedMedia.size == 1) {
                                selectedMedia.value = setOf(id)
                            } else if (selectedMedia.value.isEmpty()) {
                                selectedMedia.add(id)
                            } else {
                                selectedMedia.value = selectedMedia.value.minus(id)
                            }
                        },
                        onItemClick = {
                            view.vibrate()
                            val id = media.id
                            if (allowSelection) {
                                if (selectedMedia.value.contains(id)) selectedMedia.remove(id)
                                else selectedMedia.add(id)
                            } else if (!selectedMedia.value.contains(id) && selectedMedia.size == 1) {
                                selectedMedia.value = setOf(id)
                            } else if (selectedMedia.value.isEmpty()) {
                                selectedMedia.add(id)
                            } else {
                                selectedMedia.value = selectedMedia.value.minus(id)
                            }
                        }
                    )
                }
            }
        }
    }
}
