/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dot.gallery.ui.theme.Dimens
import com.valentinilk.shimmer.shimmer

@Composable
fun LoadingMedia(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues
) {
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        state = gridState,
        modifier = modifier
            .fillMaxSize()
            .shimmer(),
        columns = GridCells.Adaptive(Dimens.Photo()),
        contentPadding = paddingValues,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .height(24.dp)
                        .fillMaxWidth(0.45f)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(100)
                        )
                )
            }
        }
        items(count = 10) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .size(Dimens.Photo())
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .height(24.dp)
                        .fillMaxWidth(0.35f)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(100)
                        )
                )
            }
        }
        items(count = 25) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .size(Dimens.Photo())
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}