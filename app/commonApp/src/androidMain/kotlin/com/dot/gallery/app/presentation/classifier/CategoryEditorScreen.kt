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
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.feature.classifier.ui.CategoryEditorActions
import com.dot.gallery.feature.classifier.ui.CategoryEditorContent
import com.dot.gallery.feature.classifier.ui.CategoryEditorState
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.feature.classifier.CategoryEditorViewModel


/**
 * Unified screen for creating and editing categories — app shell for
 * `:feature:classifier`'s [CategoryEditorContent]. Owns the ViewModel.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CategoryEditorScreen(
    categoryId: Long? = null,
    paddingValues: PaddingValues,
    isScrolling: MutableState<Boolean>,
    metadataState: State<MediaMetadataState>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onNavigateBack: () -> Unit
) {
    val viewModel = injectViewModel<CategoryEditorViewModel> {categoryEditorViewModel}

    val category by viewModel.category.collectAsStateWithLifecycle()
    val categoryName by viewModel.categoryName.collectAsStateWithLifecycle()
    val searchTerms by viewModel.searchTerms.collectAsStateWithLifecycle()
    val threshold by viewModel.threshold.collectAsStateWithLifecycle()
    val referenceImageIds by viewModel.referenceImageIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val previewMedia by viewModel.previewMedia.collectAsStateWithLifecycle()
    val previewMediaState = viewModel.previewMediaState.collectAsStateWithLifecycle()
    val previewCount by viewModel.previewCount.collectAsStateWithLifecycle()
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()

    CategoryEditorContent(
        state = CategoryEditorState(
            categoryId = categoryId,
            category = category,
            categoryName = categoryName,
            searchTerms = searchTerms,
            threshold = threshold,
            referenceImageIds = referenceImageIds,
            isLoading = isLoading,
            isSaving = isSaving,
            previewMedia = previewMedia,
            previewCount = previewCount,
            allMedia = allMedia,
        ),
        actions = CategoryEditorActions(
            onLoadCategory = viewModel::loadCategory,
            onNameChange = viewModel::updateCategoryName,
            onSearchTermsChange = viewModel::updateSearchTerms,
            onThresholdChange = viewModel::updateThreshold,
            onApplyTemplate = viewModel::applyTemplate,
            onAddReferenceImage = viewModel::addReferenceImage,
            onRemoveReferenceImage = viewModel::removeReferenceImage,
            onSave = { onDone -> viewModel.saveCategory(onDone) },
            onDelete = { onDone -> viewModel.deleteCategory(onDone) },
            onNavigateBack = onNavigateBack,
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
