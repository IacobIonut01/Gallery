/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared inline error slot for the vault auth inputs — reserves space even when
 * [message] is null so the layout does not jump when an error appears.
 */
@Composable
internal fun VaultErrorText(message: String?) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = message.orEmpty(),
        color = if (message != null) MaterialTheme.colorScheme.error else Color.Transparent,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
