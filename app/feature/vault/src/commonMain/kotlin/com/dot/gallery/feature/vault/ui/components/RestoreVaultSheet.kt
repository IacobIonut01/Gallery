/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui.components

import androidx.compose.runtime.Composable
import com.dot.gallery.core.presentation.components.ConfirmationSheet
import com.dot.gallery.core.ui.util.AppBottomSheetState
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.vault.generated.resources.Res
import com.dot.gallery.feature.vault.generated.resources.restore_vault_summary
import com.dot.gallery.feature.vault.generated.resources.restore_vault_title

@Composable
fun RestoreVaultSheet(
    state: AppBottomSheetState,
    onConfirm: () -> Unit
) {
    ConfirmationSheet(
        state = state,
        title = stringResource(Res.string.restore_vault_title),
        summary = stringResource(Res.string.restore_vault_summary),
        onConfirm = onConfirm
    )
}
