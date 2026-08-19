/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.util.PreviewHost

@Composable
fun EmptyAlbum(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.no_media_title),
) {
    EmptyState(
        title = title,
        icon = Icons.Outlined.PhotoAlbum,
        modifier = modifier,
    )
}

@Preview(showBackground = true, name = "Empty album")
@Composable
private fun EmptyAlbumPreview() {
    PreviewHost {
        EmptyAlbum()
    }
}
