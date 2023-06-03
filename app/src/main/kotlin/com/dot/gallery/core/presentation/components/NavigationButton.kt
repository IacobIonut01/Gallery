/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R

@Composable
fun NavigationButton(
    albumId: Long,
    target: String?,
    navigateUp: () -> Unit,
    clearSelection: () -> Unit,
    selectionState: MutableState<Boolean>,
    alwaysGoBack: Boolean,
) {
    val isChildRoute = albumId != -1L || target != null
    val onClick: () -> Unit =
        if (isChildRoute && !selectionState.value) navigateUp
        else clearSelection
    val icon = if (isChildRoute && !selectionState.value) Icons.Default.ArrowBack
    else Icons.Default.Close
    if (isChildRoute || selectionState.value || alwaysGoBack) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.back_cd)
            )
        }
    }
}