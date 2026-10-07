/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.trashed

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.Settings.Misc.rememberMosaicGridSize
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.app.presentation.search.MainSearchBar
import com.dot.gallery.app.presentation.trashed.components.TrashSelectionSheet
import com.dot.gallery.core.presentation.util.Screen
import com.dot.gallery.app.presentation.trashed.components.AutoDeleteFooter
import com.dot.gallery.feature.library.ui.TrashedContent


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T: Media> TrashedGridScreen(
    paddingValues: PaddingValues,
    mediaState: State<MediaState<T>>,
    metadataState: State<MediaMetadataState>,
    clearSelection: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    TrashedContent(
        paddingValues = paddingValues,
        mediaState = mediaState,
        metadataState = metadataState,
        lastCellIndex = rememberGridSize(),
        lastMosaicCellIndex = rememberMosaicGridSize(),
        mediaViewerRoute = Screen.MediaViewScreen.route,
        searchBar = { scrolling, menuItems ->
            MainSearchBar(
                isScrolling = scrolling,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                menuItems = menuItems,
            )
        },
        selectionSheetContent = { allMedia, selectedMediaList ->
            TrashSelectionSheet(
                modifier = Modifier.align(Alignment.BottomEnd),
                allMedia = allMedia,
                selectedMedia = selectedMediaList
            )
        },
        aboveGridContent = { AutoDeleteFooter(mediaState) },
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope
    )
}
