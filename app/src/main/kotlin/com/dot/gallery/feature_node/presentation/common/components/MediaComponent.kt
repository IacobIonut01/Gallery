/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.common.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.bumptech.glide.RequestBuilder
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.ui.theme.Shapes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyGridItemScope.MediaComponent(
    media: Media,
    selectionState: MutableState<Boolean>,
    selectedMedia: SnapshotStateList<Media>,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    onItemClick: (Media) -> Unit,
    onItemLongClick: (Media) -> Unit,
) {
    val isSelected = remember { mutableStateOf(false) }
    MediaImage(
        media = media,
        preloadRequestBuilder = preloadRequestBuilder,
        selectionState = selectionState,
        selectedMedia = selectedMedia,
        isSelected = isSelected,
        modifier = Modifier
            .clip(Shapes.small)
            .animateItemPlacement()
            .combinedClickable(
                onClick = {
                    onItemClick(media)
                    if (selectionState.value) {
                        isSelected.value = !isSelected.value
                    }
                },
                onLongClick = {
                    onItemLongClick(media)
                    if (selectionState.value) {
                        isSelected.value = !isSelected.value
                    }
                },
            )
    )
}