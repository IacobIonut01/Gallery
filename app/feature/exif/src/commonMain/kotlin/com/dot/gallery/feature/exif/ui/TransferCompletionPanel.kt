/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.Media
import com.dot.gallery.feature.albums.ui.components.ProviderIconSlot
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.exif.generated.resources.Res
import com.dot.gallery.feature.exif.generated.resources.done
import com.dot.gallery.feature.exif.generated.resources.transfer_complete_summary
import com.dot.gallery.feature.exif.generated.resources.transfer_copy_complete
import com.dot.gallery.feature.exif.generated.resources.transfer_move_complete
import com.dot.gallery.feature.exif.generated.resources.transfer_original_retained

@Composable
fun TransferCompletionPanel(
    media: List<Media>,
    destination: Album?,
    destinationLabel: String,
    mode: TransferSheetMode,
    sourceRetained: Boolean,
    openLabel: String,
    onOpen: (() -> Unit)?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    providerIcon: ProviderIconSlot? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(
                when {
                    sourceRetained -> Res.string.transfer_original_retained
                    mode == TransferSheetMode.MOVE -> Res.string.transfer_move_complete
                    else -> Res.string.transfer_copy_complete
                }
            ),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (media.size == 1) destinationLabel else stringResource(
                Res.string.transfer_complete_summary,
                media.size,
                destinationLabel
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TransferJourneyPreview(
            media = media,
            destination = destination,
            destinationLabel = destinationLabel,
            mode = mode,
            progress = 1f,
            showProgress = false,
            showStatus = false,
            providerIcon = providerIcon
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            TextButton(onClick = onDone) {
                Text(stringResource(Res.string.done))
            }
            if (onOpen != null) {
                Button(onClick = onOpen) {
                    Text(openLabel)
                }
            }
        }
    }
}
