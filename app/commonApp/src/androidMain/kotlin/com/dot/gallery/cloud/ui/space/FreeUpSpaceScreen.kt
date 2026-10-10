/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.space

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.data.repository.MediaMutationResult
import com.dot.gallery.core.ui.util.rememberActivityResult
import com.dot.gallery.injection.injectViewModel


@Composable
fun FreeUpSpaceScreen() {
    val viewModel = injectViewModel<FreeUpSpaceViewModel> {freeUpSpaceViewModel(it)}
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val handler = LocalMediaHandler.current
    val deletionResult = rememberActivityResult(
        onResultCanceled = viewModel::cancelLocalDeletion,
        onResultOk = viewModel::completeLocalDeletionBatch
    )
    LaunchedEffect(
        state.isDeleting,
        state.isPreparingDeletionBatch,
        state.isDeletionRequestPending,
        state.deletionCandidates,
        state.pendingDeletionItems
    ) {
        if (!state.isDeleting || state.isDeletionRequestPending) return@LaunchedEffect
        if (state.pendingDeletionItems.isEmpty()) {
            if (!state.isPreparingDeletionBatch && state.deletionCandidates.isNotEmpty()) {
                viewModel.prepareNextDeletionBatch()
            }
            return@LaunchedEffect
        }
        when (handler.deleteMedia(deletionResult, state.pendingDeletionItems)) {
            MediaMutationResult.COMPLETED -> viewModel.completeLocalDeletionBatch()
            MediaMutationResult.REQUEST_LAUNCHED -> viewModel.markDeletionRequestPending()
            MediaMutationResult.FAILED -> viewModel.failLocalDeletion()
        }
    }

    FreeUpSpaceContent(
        state = state,
        onKeepFavorites = viewModel::setKeepFavorites,
        onCutoffDays = viewModel::setCutoffDays,
        onAutoEnabled = viewModel::setAutoEnabled,
        onAutoIntervalDays = viewModel::setAutoIntervalDays,
        onScan = viewModel::scan,
        onBeginDeletion = viewModel::beginLocalDeletion,
        onCancelDeletion = viewModel::cancelLocalDeletion,
    )
}
