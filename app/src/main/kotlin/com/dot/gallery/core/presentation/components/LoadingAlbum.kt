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
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.feature_node.presentation.util.PreviewHost
import com.dot.gallery.ui.theme.ComponentSize
import com.dot.gallery.ui.theme.Dimens
import com.dot.gallery.ui.theme.Spacing
import com.valentinilk.shimmer.shimmer

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

@Composable
internal fun LoadingAlbumContent(
    grid: Int,
    canShimmer: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.ScreenHorizontal)
            .padding(top = ComponentSize.MinimumTouchTarget)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .then(if (canShimmer) Modifier.shimmer() else Modifier),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                repeat(grid) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .size(Dimens.Album())
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = shape
                            )
                    )
                }
            }
        }
        repeat(4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                repeat(grid) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .size(Dimens.Album())
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = shape
                            )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Loading albums")
@Composable
private fun LoadingAlbumPreview() {
    PreviewHost {
        LoadingAlbumContent(grid = 2, canShimmer = false)
    }
}