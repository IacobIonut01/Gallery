/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.R

/** The user's choice for how to save a transparent cut-out edit. */
enum class CutoutSaveChoice { TRANSPARENT_PNG, FLATTEN_WHITE, FLATTEN_BLACK }

/**
 * Bottom sheet shown when saving an image that contains a background-removal (cut-out) edit. JPEG
 * and most source formats can't store transparency, so the user picks between a transparent PNG or
 * flattening the subject onto a solid colour (keeping the source format).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutoutSaveChoiceSheet(
    onChoice: (CutoutSaveChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.cutout_save_choice_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            ChoiceRow(
                icon = Icons.Outlined.Image,
                label = stringResource(R.string.cutout_save_transparent_png),
                onClick = { onChoice(CutoutSaveChoice.TRANSPARENT_PNG) },
            )
            Spacer(Modifier.height(8.dp))
            ChoiceRow(
                icon = Icons.Outlined.FormatColorFill,
                label = stringResource(R.string.cutout_save_flatten_white),
                onClick = { onChoice(CutoutSaveChoice.FLATTEN_WHITE) },
            )
            Spacer(Modifier.height(8.dp))
            ChoiceRow(
                icon = Icons.Outlined.FormatColorFill,
                label = stringResource(R.string.cutout_save_flatten_black),
                onClick = { onChoice(CutoutSaveChoice.FLATTEN_BLACK) },
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
