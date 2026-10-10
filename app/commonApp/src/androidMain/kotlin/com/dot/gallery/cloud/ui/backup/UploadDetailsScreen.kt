/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.injection.injectViewModel


@Composable
fun UploadDetailsScreen() {
    val viewModel = injectViewModel<UploadDetailsViewModel> {uploadDetailsViewModel}
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UploadDetailsContent(state = state)
}
