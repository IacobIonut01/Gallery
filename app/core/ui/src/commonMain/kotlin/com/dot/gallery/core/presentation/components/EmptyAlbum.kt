/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dot.gallery.core.ui.components.PreviewHost
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.dot.gallery.core.ui.generated.resources.Res
import com.dot.gallery.core.ui.generated.resources.no_media_title

@Composable
fun EmptyAlbum(
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.no_media_title),
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
