/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.dot.gallery.core.ui.components.PreviewHost
import com.dot.gallery.core.ui.util.shimmerLoading
import com.dot.gallery.ui.theme.ComponentSize
import com.dot.gallery.ui.theme.Dimens
import com.dot.gallery.ui.theme.Spacing
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LoadingAlbumContent(
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
            .then(if (canShimmer) Modifier.shimmerLoading() else Modifier),
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
