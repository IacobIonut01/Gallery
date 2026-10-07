/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.classifier

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.navigate
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.feature.classifier.ui.EditCategoryActions
import com.dot.gallery.feature.classifier.ui.EditCategoryContent
import com.dot.gallery.feature.classifier.ui.EditCategoryState
import com.dot.gallery.core.presentation.util.Screen
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.feature.classifier.EditCategoryViewModel


/**
 * App shell for `:feature:classifier`'s [EditCategoryContent] — owns the
 * ViewModel and wires routes through `Screen`.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EditCategoryScreen(
    categoryId: Long,
    paddingValues: PaddingValues,
    isScrolling: MutableState<Boolean>,
    metadataState: State<MediaMetadataState>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onNavigateBack: () -> Unit
) {
    val viewModel = injectViewModel<EditCategoryViewModel> {editCategoryViewModel}
    val eventHandler = LocalEventHandler.current

    val category by viewModel.category.collectAsStateWithLifecycle()
    val categoryName by viewModel.categoryName.collectAsStateWithLifecycle()
    val searchTerms by viewModel.searchTerms.collectAsStateWithLifecycle()
    val threshold by viewModel.threshold.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val previewMediaState by viewModel.previewMediaState.collectAsStateWithLifecycle()
    val previewCount by viewModel.previewCount.collectAsStateWithLifecycle()

    EditCategoryContent(
        state = EditCategoryState(
            categoryId = categoryId,
            category = category,
            categoryName = categoryName,
            searchTerms = searchTerms,
            threshold = threshold,
            isLoading = isLoading,
            isSaving = isSaving,
            previewCount = previewCount,
        ),
        actions = EditCategoryActions(
            onLoadCategory = viewModel::loadCategory,
            onNameChange = viewModel::updateCategoryName,
            onSearchTermsChange = viewModel::updateSearchTerms,
            onThresholdChange = viewModel::updateThreshold,
            onSave = { onDone -> viewModel.saveCategory(onDone) },
            onDelete = { onDone -> viewModel.deleteCategory(onDone) },
            onNavigateBack = onNavigateBack,
            onOpenPreviewMedia = { mediaId ->
                eventHandler.navigate(Screen.MediaViewScreen.idAndCategoryId(mediaId, categoryId))
            },
        ),
        previewMediaState = previewMediaState,
        metadataState = metadataState,
        isScrolling = isScrolling,
        lastCellIndex = rememberGridSize(),
        paddingValues = paddingValues,
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
    )
}
