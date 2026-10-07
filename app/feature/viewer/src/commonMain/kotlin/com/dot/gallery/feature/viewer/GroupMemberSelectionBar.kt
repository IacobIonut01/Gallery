/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.select_all
import com.dot.gallery.feature.viewer.generated.resources.selection_dialog_close_cd
import com.dot.gallery.feature.viewer.generated.resources.share


@Composable
fun GroupMemberSelectionBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                shape = shape
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.selection_dialog_close_cd),
                tint = Color.White
            )
        }
        Text(
            text = selectedCount.toString(),
            color = Color.White,
            style = MaterialTheme.typography.titleSmall
        )
        IconButton(onClick = onSelectAll) {
            Icon(
                imageVector = Icons.Outlined.SelectAll,
                contentDescription = stringResource(Res.string.select_all),
                tint = Color.White
            )
        }
        IconButton(
            onClick = onShare,
            enabled = selectedCount > 0
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(Res.string.share),
                tint = if (selectedCount > 0) Color.White else Color.White.copy(alpha = 0.38f)
            )
        }
    }
}
