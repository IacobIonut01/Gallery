/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.classifier

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.Settings.Misc.rememberMosaicGridSize
import com.dot.gallery.core.navigate
import com.dot.gallery.core.navigateUp
import com.dot.gallery.core.presentation.components.SelectionSheet
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.feature.classifier.ui.CategoryViewActions
import com.dot.gallery.feature.classifier.ui.CategoryViewState
import com.dot.gallery.core.presentation.util.Screen
import com.dot.gallery.feature.classifier.ui.CategoryViewContent
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.feature.classifier.CategoryViewModel


/**
 * Legacy CategoryViewScreen for string-based categories — app shell for
 * `:feature:classifier`'s [CategoryViewContent].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CategoryViewScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    metadataState: State<MediaMetadataState>,
    category: String
) {
    val eventHandler = LocalEventHandler.current
    val viewModel = injectViewModel<CategoryViewModel> {categoryViewModel}.apply {
        this.category = category
    }

    val mediaState = viewModel.mediaByCategory.collectAsStateWithLifecycle()

    CategoryViewContent(
        state = CategoryViewState(legacyCategory = category),
        actions = CategoryViewActions(
            onInvalidCategory = { eventHandler.navigateUp() },
            onLoadCategory = {},
            onEditCategory = {},
            onOpenMedia = { mediaId ->
                eventHandler.navigate(Screen.MediaViewScreen.idAndCategory(mediaId, category))
            },
            onSearchClick = { eventHandler.navigate(Screen.SearchScreen()) },
        ),
        mediaState = mediaState,
        metadataState = metadataState,
        mediaViewerRoute = Screen.MediaViewScreen.route,
        isScrolling = remember { mutableStateOf(false) },
        lastCellIndex = rememberGridSize(),
        lastMosaicCellIndex = rememberMosaicGridSize(),
        selectionSheetContent = { allMedia, selectedMediaList ->
            SelectionSheet(
                modifier = Modifier.align(Alignment.BottomEnd),
                allMedia = allMedia,
                selectedMedia = selectedMediaList
            )
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope
    )
}

/**
 * New CategoryViewScreen for ID-based categories (new embedding system) —
 * app shell for `:feature:classifier`'s [CategoryViewContent].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CategoryViewScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    metadataState: State<MediaMetadataState>,
    categoryId: Long
) {
    val eventHandler = LocalEventHandler.current
    val viewModel = injectViewModel<CategoryViewModel> {categoryViewModel}

    val mediaState = viewModel.mediaByCategoryId.collectAsStateWithLifecycle()
    val category by viewModel.categoryDetails.collectAsStateWithLifecycle()

    CategoryViewContent(
        state = CategoryViewState(
            categoryId = categoryId,
            categoryName = category?.name ?: "",
        ),
        actions = CategoryViewActions(
            onInvalidCategory = { eventHandler.navigateUp() },
            onLoadCategory = viewModel::setCategoryId,
            onEditCategory = { id ->
                eventHandler.navigate(Screen.CategoryEditorScreen.edit(id))
            },
            onOpenMedia = { mediaId ->
                eventHandler.navigate(Screen.MediaViewScreen.idAndCategoryId(mediaId, categoryId))
            },
            onSearchClick = { eventHandler.navigate(Screen.SearchScreen()) },
        ),
        mediaState = mediaState,
        metadataState = metadataState,
        mediaViewerRoute = Screen.MediaViewScreen.route,
        isScrolling = remember { mutableStateOf(false) },
        lastCellIndex = rememberGridSize(),
        lastMosaicCellIndex = rememberMosaicGridSize(),
        selectionSheetContent = { allMedia, selectedMediaList ->
            SelectionSheet(
                modifier = Modifier.align(Alignment.BottomEnd),
                allMedia = allMedia,
                selectedMedia = selectedMediaList
            )
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope
    )
}
