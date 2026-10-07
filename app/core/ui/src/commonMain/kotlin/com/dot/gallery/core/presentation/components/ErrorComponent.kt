/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.dot.gallery.core.ui.util.rememberPlatformToast
import com.dot.gallery.ui.theme.ComponentSize
import com.dot.gallery.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.core.ui.generated.resources.Res
import com.dot.gallery.core.ui.generated.resources.copy_error
import com.dot.gallery.core.ui.generated.resources.error_copied
import com.dot.gallery.core.ui.generated.resources.error_title
import com.dot.gallery.core.ui.generated.resources.retry


@Composable
fun Error(
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.error_title),
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val retryLabel = stringResource(Res.string.retry)
    val copiedLabel = stringResource(Res.string.error_copied)
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val toast = rememberPlatformToast()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium, Alignment.CenterVertically),
    ) {
        Icon(
            modifier = Modifier.size(ComponentSize.StateIcon),
            imageVector = Icons.Outlined.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        if (!errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (onRetry != null || onAction != null || !errorMessage.isNullOrEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!errorMessage.isNullOrEmpty()) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(errorMessage))
                            toast(copiedLabel)
                        },
                    ) {
                        Text(text = stringResource(Res.string.copy_error))
                    }
                }
                onRetry?.let {
                    Button(onClick = it) {
                        Text(text = retryLabel)
                    }
                }
                onAction?.let {
                    Button(onClick = it) {
                        Text(text = actionLabel ?: retryLabel)
                    }
                }
            }
        }
    }
}
