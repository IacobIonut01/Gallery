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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.tooling.preview.Preview
import com.dot.gallery.core.Constants.cellsList
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.feature_node.presentation.util.PreviewHost
import com.dot.gallery.ui.theme.Dimens
import com.dot.gallery.ui.theme.Spacing
import com.valentinilk.shimmer.shimmer

@Composable
fun LoadingMedia(
    modifier: Modifier = Modifier,
) {
    val gridSize by rememberGridSize()
    val grid = remember(gridSize) { cellsList.size - gridSize }
    val canShimmer = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
    LoadingMediaContent(
        grid = grid,
        canShimmer = canShimmer,
        modifier = modifier,
    )
}

@Composable
internal fun LoadingMediaContent(
    grid: Int,
    canShimmer: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .then(if (canShimmer) Modifier.shimmer() else Modifier),
        verticalArrangement = Arrangement.spacedBy(Spacing.Hairline),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = Spacing.Large, vertical = Spacing.Large),
        ) {
            Spacer(
                modifier = Modifier
                    .height(Spacing.Large)
                    .fillMaxWidth(0.45f)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Hairline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(grid) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .size(Dimens.Photo())
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Hairline)
        ) {
            repeat(grid / 2) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .size(Dimens.Photo())
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
            repeat(grid / 2) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .size(Dimens.Photo())
                )
            }
        }
        Box(
            modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Large)
        ) {
            Spacer(
                modifier = Modifier
                    .height(Spacing.Large)
                    .fillMaxWidth(0.45f)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
            )
        }
        repeat(10) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Hairline),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(grid) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .size(Dimens.Photo())
                            .background(color = MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Hairline)
        ) {
            repeat(grid / 2) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .size(Dimens.Photo())
                        .background(color = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            repeat(grid / 2) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .size(Dimens.Photo())
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Loading media")
@Composable
private fun LoadingMediaPreview() {
    PreviewHost {
        LoadingMediaContent(grid = 3, canShimmer = false)
    }
}