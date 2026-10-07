/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.app.presentation.collection

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.Settings.Misc.rememberMosaicGridSize
import com.dot.gallery.core.navigate
import com.dot.gallery.core.presentation.components.SelectionSheet
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.feature.albums.ui.collections.CollectionViewContent
import com.dot.gallery.core.presentation.util.Screen

/**
 * App-side shell for the collection media grid: delegates to
 * `:feature:albums` [CollectionViewContent] and bridges the persisted grid
 * sizes, media-view navigation and the app-owned [SelectionSheet] overlay.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionViewScreen(
    collectionId: Long,
    collectionName: String,
    paddingValues: PaddingValues,
    isScrolling: MutableState<Boolean>,
    metadataState: State<MediaMetadataState>,
    onEditAlbums: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    val eventHandler = LocalEventHandler.current
    CollectionViewContent(
        collectionId = collectionId,
        collectionName = collectionName,
        paddingValues = paddingValues,
        isScrolling = isScrolling,
        metadataState = metadataState,
        gridSizeIndex = rememberGridSize(),
        mosaicGridSizeIndex = rememberMosaicGridSize(),
        onMediaClick = { media ->
            eventHandler.navigate(Screen.MediaViewScreen.idAndCollection(media.id, collectionId))
        },
        onEditAlbums = onEditAlbums,
        selectionSheet = { mediaState, selectedMedia ->
            SelectionSheet(
                modifier = Modifier.align(Alignment.BottomEnd),
                allMedia = mediaState,
                selectedMedia = selectedMedia,
                collectionId = collectionId
            )
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
    )
}
