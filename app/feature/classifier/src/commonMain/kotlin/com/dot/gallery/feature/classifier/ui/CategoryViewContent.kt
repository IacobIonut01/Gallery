/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.dot.gallery.core.presentation.components.MainSearchBar
import com.dot.gallery.core.presentation.components.MediaScreen
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.classifier.generated.resources.Res
import com.dot.gallery.feature.classifier.generated.resources.edit_category
import com.dot.gallery.feature.classifier.generated.resources.s_items


/**
 * Media grid for a single category — supports both the legacy string route
 * ([CategoryViewState.legacyCategory] non-null) and the id-based route
 * ([CategoryViewState.categoryId] > 0).
 *
 * @param mediaState resolved per route (`mediaByCategory` / `mediaByCategoryId`)
 * @param mediaViewerRoute route prefix for the viewer — only used when
 * [CategoryViewActions.onOpenMedia] does not handle the click (it always does).
 * @param selectionSheetContent platform selection overlay slot.
 * @param lastCellIndex / [lastMosaicCellIndex] persisted grid cell indices
 * (platform store — app-side).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T : Media> CategoryViewContent(
    state: CategoryViewState,
    actions: CategoryViewActions,
    mediaState: State<MediaState<T>>,
    metadataState: State<MediaMetadataState>,
    mediaViewerRoute: String,
    isScrolling: MutableState<Boolean>,
    lastCellIndex: MutableState<Int>,
    lastMosaicCellIndex: MutableState<Int>,
    selectionSheetContent: @Composable BoxScope.(
        allMedia: MediaState<T>,
        selectedMediaList: SnapshotStateList<T>,
    ) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    LaunchedEffect(state.legacyCategory, state.categoryId) {
        if (state.isLegacy) {
            if (state.legacyCategory.isNullOrEmpty()) actions.onInvalidCategory()
        } else {
            if (state.categoryId <= 0) actions.onInvalidCategory()
            else actions.onLoadCategory(state.categoryId)
        }
    }

    MediaScreen(
        albumName = state.displayName,
        customDateHeader = stringResource(Res.string.s_items, mediaState.value.media.size),
        mediaState = mediaState,
        metadataState = metadataState,
        target = state.target,
        mediaViewerRoute = mediaViewerRoute,
        isScrolling = isScrolling,
        navActionsContent = {
            if (!state.isLegacy) {
                IconButton(onClick = { actions.onEditCategory(state.categoryId) }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(Res.string.edit_category)
                    )
                }
            }
        },
        selectionSheetContent = selectionSheetContent,
        customViewingNavigation = { media ->
            actions.onOpenMedia(media.id)
        },
        lastCellIndex = lastCellIndex,
        lastMosaicCellIndex = lastMosaicCellIndex,
        searchBar = { scrolling, menuItems ->
            MainSearchBar(
                isScrolling = scrolling,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
                onSearchClick = actions.onSearchClick,
                menuItems = menuItems,
            )
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope
    )
}
