/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components.media

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bumptech.glide.RequestBuilder
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.ui.theme.Shapes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyGridItemScope.MediaComponent(
    media: Media,
    selectionState: MutableState<Boolean>,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    onItemClick: (Media) -> Unit,
    onItemLongClick: (Media) -> Unit,
    isSelected: Boolean,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(
                width = .2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = Shapes.small
            )
            .clip(Shapes.small)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
            )
            .animateItemPlacement()
            .combinedClickable(
                onClick = { onItemClick(media) },
                onLongClick = { onItemLongClick(media) },
            ),
    ) {
        MediaImage(
            media = media,
            preloadRequestBuilder = preloadRequestBuilder,
            selectionState = selectionState,
            isSelected = isSelected,
        )
    }
}