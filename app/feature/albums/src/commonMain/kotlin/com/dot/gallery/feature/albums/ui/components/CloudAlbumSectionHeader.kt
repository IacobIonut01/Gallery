/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.albums.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dot.gallery.cloud.core.ProviderType
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.albums.generated.resources.Res
import com.dot.gallery.feature.albums.generated.resources.n_albums

/**
 * Header for the dedicated cloud albums section, showing the provider's brand
 * logo (falling back to a generic cloud glyph) and its display name.
 */
@Composable
fun CloudAlbumSectionHeader(
    modifier: Modifier = Modifier,
    providerName: String,
    count: Int,
    providerIcon: ProviderIconSlot? = null,
) {
    val providerType = remember(providerName) {
        try { ProviderType.valueOf(providerName) } catch (_: Exception) { null }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (providerType != null && providerIcon != null) {
            providerIcon(
                providerType,
                MaterialTheme.colorScheme.onSurface,
                Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = providerType?.displayName ?: providerName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(Res.string.n_albums, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
