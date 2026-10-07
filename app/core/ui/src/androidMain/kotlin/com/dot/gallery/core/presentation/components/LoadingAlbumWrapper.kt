/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.valentinilk.shimmer.shimmer
import com.dot.gallery.core.presentation.components.LoadingAlbumContent
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize


@Composable
fun LoadingAlbum(
    modifier: Modifier = Modifier,
) {
    val gridSize by rememberAlbumGridSize()
    val grid = remember(gridSize) { albumCellsList.size - gridSize }
    val canShimmer = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
    LoadingAlbumContent(
        grid = grid,
        canShimmer = canShimmer,
        modifier = modifier,
    )
}
