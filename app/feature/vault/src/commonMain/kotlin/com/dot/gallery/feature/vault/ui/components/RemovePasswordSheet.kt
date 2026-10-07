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
import com.dot.gallery.feature.vault.generated.resources.vault_remove_custom_password
import com.dot.gallery.feature.vault.generated.resources.vault_remove_custom_password_summary

@Composable
fun RemovePasswordSheet(
    state: AppBottomSheetState,
    onConfirm: () -> Unit
) {
    ConfirmationSheet(
        state = state,
        title = stringResource(Res.string.vault_remove_custom_password),
        summary = stringResource(Res.string.vault_remove_custom_password_summary),
        onConfirm = onConfirm
    )
}
