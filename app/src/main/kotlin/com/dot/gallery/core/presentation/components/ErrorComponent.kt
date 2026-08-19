/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("DEPRECATION")

package com.dot.gallery.core.presentation.components

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.util.PreviewHost
import com.dot.gallery.ui.theme.ComponentSize
import com.dot.gallery.ui.theme.Spacing

@Composable
fun Error(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.error_title),
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val retryLabel = stringResource(R.string.retry)
    val copiedLabel = stringResource(R.string.error_copied)
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
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
                            Toast.makeText(
                                context,
                                copiedLabel,
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    ) {
                        Text(text = stringResource(R.string.copy_error))
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

@Preview(showBackground = true, name = "Error with retry")
@Composable
private fun ErrorWithActionPreview() {
    PreviewHost {
        Error(
            errorMessage = "The media library could not be loaded.",
            onRetry = {},
        )
    }
}
