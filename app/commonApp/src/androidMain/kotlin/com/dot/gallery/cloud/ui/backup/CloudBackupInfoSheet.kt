/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.request.ComposableImageRequest
import com.dot.gallery.cloud.core.CloudUri
import com.dot.gallery.cloud.ui.descriptor.ProviderBrandIcon
import com.dot.gallery.core.presentation.components.ModalSheet
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.feature.editor.adjustments.Crop
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_backed_up_summary
import com.dot.gallery.generated.resources.cloud_backed_up_title
import com.dot.gallery.generated.resources.cloud_backup_delete
import com.dot.gallery.generated.resources.cloud_media


/**
 * Lists every cloud account holding a backup copy of the currently-viewed local media.
 * Each row shows the provider brand icon, a thumbnail, the provider name, and a delete
 * action that removes just that remote copy (the local original is untouched).
 */
@Composable
fun CloudBackupInfoSheet(
    sheetState: AppBottomSheetState,
    backups: List<UriMedia>,
    viewModel: CloudBackupInfoViewModel = injectViewModel { cloudBackupInfoViewModel }
) {
    val deletingIds by viewModel.deletingIds.collectAsStateWithLifecycle()
    ModalSheet(
        sheetState = sheetState,
        title = stringResource(Res.string.cloud_backed_up_title),
        subtitle = stringResource(Res.string.cloud_backed_up_summary, backups.size),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                backups.forEach { backup ->
                    val providerType = CloudUri.parse(backup.uri.toString())?.providerType
                    val isDeleting = backup.id in deletingIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            request = ComposableImageRequest(backup.uri.toString()),
                            contentDescription = backup.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (providerType != null) {
                                    ProviderBrandIcon(
                                        providerType = providerType,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = providerType?.displayName
                                        ?: stringResource(Res.string.cloud_media),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = backup.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = { viewModel.deleteBackup(backup) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = stringResource(Res.string.cloud_backup_delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
