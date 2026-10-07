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
import com.dot.gallery.feature.vault.generated.resources.vault_deletion_summary
import com.dot.gallery.feature.vault.generated.resources.vault_deletion_summary_named
import com.dot.gallery.feature.vault.generated.resources.vault_deletion_title
import com.dot.gallery.feature.vault.generated.resources.vault_deletion_title_named

@Composable
fun DeleteVaultSheet(
    state: AppBottomSheetState,
    vaultName: String? = null,
    onConfirm: () -> Unit
) {
    ConfirmationSheet(
        state = state,
        title = if (vaultName != null) stringResource(Res.string.vault_deletion_title_named, vaultName)
                else stringResource(Res.string.vault_deletion_title),
        summary = if (vaultName != null) stringResource(Res.string.vault_deletion_summary_named, vaultName)
                  else stringResource(Res.string.vault_deletion_summary),
        onConfirm = onConfirm
    )
}
