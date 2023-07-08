/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.presentation.timeline.components

import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Settings.Misc.rememberIsMediaManager
import com.dot.gallery.feature_node.presentation.util.launchManageMedia

@Composable
fun RequestMediaManager() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
        var useMediaManager by rememberIsMediaManager()
        val context = LocalContext.current
        AnimatedVisibility(visible = !useMediaManager && !MediaStore.canManageMedia(context)) {
            Column(
                modifier = Modifier.padding(16.dp).padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_media_manage),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                            .padding(8.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.manage_media_title),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = stringResource(R.string.manage_media_summary),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Row {
                    OutlinedButton(
                        onClick = { useMediaManager = true },
                        border = null
                    ) {
                        Text(text = stringResource(R.string.skip))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            context.launchManageMedia()
                            useMediaManager = true
                        }
                    ) {
                        Text(text = stringResource(R.string.allow))
                    }
                }
            }
        }
    }
}