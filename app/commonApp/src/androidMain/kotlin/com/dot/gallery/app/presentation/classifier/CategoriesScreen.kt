/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.classifier

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.core.navigate
import com.dot.gallery.data.dao.CategoryWithMediaCount
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.feature.classifier.ui.CategoriesActions
import com.dot.gallery.feature.classifier.ui.CategoriesContent
import com.dot.gallery.feature.classifier.ui.CategoriesState
import com.dot.gallery.core.presentation.util.Screen


/**
 * App shell for `:feature:classifier`'s [CategoriesContent] — collects state in
 * NavigationComp and wires route navigation through `Screen`.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CategoriesScreen(
    categoriesWithCount: List<CategoryWithMediaCount>,
    distinctMediaCount: Int,
    mediaState: MediaState<UriMedia>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    val eventHandler = LocalEventHandler.current
    CategoriesContent(
        state = CategoriesState(
            categoriesWithCount = categoriesWithCount,
            distinctMediaCount = distinctMediaCount,
            mediaState = mediaState,
        ),
        actions = CategoriesActions(
            onOpenCategory = { categoryId ->
                eventHandler.navigate(Screen.CategoryViewScreen.categoryId(categoryId))
            },
            onEditCategory = { categoryId ->
                eventHandler.navigate(Screen.CategoryEditorScreen.edit(categoryId))
            },
            onCreateCategory = {
                eventHandler.navigate(Screen.CategoryEditorScreen.create())
            },
            onOpenSettings = {
                eventHandler.navigate(Screen.CategoriesSettingsScreen())
            },
        ),
        lastCellIndex = rememberAlbumGridSize(),
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
    )
}
