/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature.viewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.runtime.Composable
import com.dot.gallery.domain.model.Media
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.add_to_collection


/**
 * Quick-actions-bar entry point for adding media to a collection. The collection
 * picker sheet is app-side; [onAddToCollection] notifies the caller to show it.
 */
@Composable
fun <T : Media> AddToCollectionButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
    onAddToCollection: (T) -> Unit
) {
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.Collections,
        followTheme = followTheme,
        title = stringResource(Res.string.add_to_collection),
        enabled = enabled,
        onItemClick = onAddToCollection
    )
}
