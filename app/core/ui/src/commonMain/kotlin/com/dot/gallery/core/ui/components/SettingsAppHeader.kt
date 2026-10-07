/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.ui.util.LocalAppBuildInfo
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.core.ui.generated.resources.Res
import com.dot.gallery.core.ui.generated.resources.action_cancel
import com.dot.gallery.core.ui.generated.resources.app_dev
import com.dot.gallery.core.ui.generated.resources.app_dev_name
import com.dot.gallery.core.ui.generated.resources.app_name
import com.dot.gallery.core.ui.generated.resources.dismiss_banner_confirm
import com.dot.gallery.core.ui.generated.resources.dismiss_banner_message
import com.dot.gallery.core.ui.generated.resources.dismiss_banner_title
import com.dot.gallery.core.ui.generated.resources.donate
import com.dot.gallery.core.ui.generated.resources.donate_button_cd
import com.dot.gallery.core.ui.generated.resources.github
import com.dot.gallery.core.ui.generated.resources.github_button_cd
import com.dot.gallery.core.ui.generated.resources.github_url
import com.dot.gallery.core.ui.generated.resources.ic_donate
import com.dot.gallery.core.ui.generated.resources.ic_github
import com.dot.gallery.core.ui.generated.resources.restore_banner_confirm
import com.dot.gallery.core.ui.generated.resources.restore_banner_message
import com.dot.gallery.core.ui.generated.resources.restore_banner_title
import com.dot.gallery.core.ui.generated.resources.version_build_format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppHeader(
    onDismiss: (() -> Unit)? = null,
    onSupportClick: () -> Unit = {},
    developerModeEnabled: Boolean = false,
    onDeveloperUnlocked: () -> Unit = {},
) {
    val appName = stringResource(Res.string.app_name)
    val buildInfo = LocalAppBuildInfo.current
    val appVersion = stringResource(
        Res.string.version_build_format,
        buildInfo.versionName,
        buildInfo.versionCode,
    )
    val appDeveloper = stringResource(Res.string.app_dev, stringResource(Res.string.app_dev_name))

    val donateImage = painterResource(Res.drawable.ic_donate)
    val donateTitle = stringResource(Res.string.donate)
    val donateContentDesc = stringResource(Res.string.donate_button_cd)

    val githubImage = painterResource(Res.drawable.ic_github)
    val githubTitle = stringResource(Res.string.github)
    val githubContentDesc = stringResource(Res.string.github_button_cd)
    val githubUrl = stringResource(Res.string.github_url)

    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val colorPrimary = MaterialTheme.colorScheme.primaryContainer
    val colorTertiary = MaterialTheme.colorScheme.tertiaryContainer

    val transition = rememberInfiniteTransition()
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000),
            repeatMode = RepeatMode.Reverse
        )
    )
    val cornerRadius = 24.dp

    val dismissSheetState = rememberAppBottomSheetState()
    val dismissTitle = stringResource(Res.string.dismiss_banner_title)
    val dismissMessage = stringResource(Res.string.dismiss_banner_message)
    val dismissConfirm = stringResource(Res.string.dismiss_banner_confirm)
    val dismissCancel = stringResource(Res.string.action_cancel)

    if (dismissSheetState.isVisible) {
        ModalBottomSheet(
            sheetState = dismissSheetState.sheetState,
            onDismissRequest = {
                scope.launch { dismissSheetState.hide() }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = dismissTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dismissMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        scope.launch {
                            dismissSheetState.hide()
                            onDismiss?.invoke()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = dismissConfirm)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch { dismissSheetState.hide() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = dismissCancel)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .clickable(enabled = onDismiss != null) {
                    scope.launch { dismissSheetState.show() }
                }
                .drawWithCache {
                    val cx = size.width - size.width * fraction
                    val cy = size.height * fraction

                    val gradient = Brush.radialGradient(
                        colors = listOf(colorPrimary, colorTertiary),
                        center = Offset(cx, cy),
                        radius = 800f
                    )

                    onDrawBehind {
                        drawRoundRect(
                            brush = gradient,
                            cornerRadius = CornerRadius(
                                cornerRadius.toPx(),
                                cornerRadius.toPx()
                            )
                        )
                    }
                }
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .padding(all = 24.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = appName,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column {
                    var unlockProgress by remember { mutableFloatStateOf(0f) }
                    Text(
                        text = appVersion,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = 6.0f
                            }
                            .holdToUnlock(
                                enabled = developerModeEnabled,
                                onUnlocked = onDeveloperUnlocked,
                                onProgress = { unlockProgress = it },
                            )
                    )
                    if (unlockProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { unlockProgress },
                            modifier = Modifier
                                .graphicsLayer { translationX = 6.0f }
                                .padding(top = 2.dp)
                                .width(96.dp)
                                .height(3.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = appDeveloper,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSupportClick() },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .12f),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f),
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .weight(1f)
                        .semantics {
                            contentDescription = donateContentDesc
                        }
                ) {
                    Icon(painter = donateImage, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = donateTitle)
                }
                Button(
                    onClick = { uriHandler.openUri(githubUrl) },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        disabledContentColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = .12f),
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                        disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = .12f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .semantics {
                            contentDescription = githubContentDesc
                        }
                ) {
                    Icon(painter = githubImage, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = githubTitle)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppHeaderCompact(
    onRestore: () -> Unit,
    onSupportClick: () -> Unit = {},
    developerModeEnabled: Boolean = false,
    onDeveloperUnlocked: () -> Unit = {},
) {
    val buildInfo = LocalAppBuildInfo.current
    val appVersion = stringResource(
        Res.string.version_build_format,
        buildInfo.versionName,
        buildInfo.versionCode,
    )

    val donateImage = painterResource(Res.drawable.ic_donate)
    val donateTitle = stringResource(Res.string.donate)
    val donateContentDesc = stringResource(Res.string.donate_button_cd)

    val githubImage = painterResource(Res.drawable.ic_github)
    val githubTitle = stringResource(Res.string.github)
    val githubContentDesc = stringResource(Res.string.github_button_cd)
    val githubUrl = stringResource(Res.string.github_url)

    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val restoreSheetState = rememberAppBottomSheetState()
    val restoreTitle = stringResource(Res.string.restore_banner_title)
    val restoreMessage = stringResource(Res.string.restore_banner_message)
    val restoreConfirm = stringResource(Res.string.restore_banner_confirm)
    val restoreCancel = stringResource(Res.string.action_cancel)

    if (restoreSheetState.isVisible) {
        ModalBottomSheet(
            sheetState = restoreSheetState.sheetState,
            onDismissRequest = {
                scope.launch { restoreSheetState.hide() }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = restoreTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = restoreMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        scope.launch {
                            restoreSheetState.hide()
                            onRestore()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = restoreConfirm)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch { restoreSheetState.hide() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = restoreCancel)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { onSupportClick() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = donateContentDesc
                        }
                ) {
                    Icon(
                        painter = donateImage,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = donateTitle, style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(
                    onClick = { uriHandler.openUri(githubUrl) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = githubContentDesc
                        }
                ) {
                    Icon(
                        painter = githubImage,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = githubTitle, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            var unlockProgress by remember { mutableFloatStateOf(0f) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .combinedClickable(
                            onClick = {
                                scope.launch { restoreSheetState.show() }
                            },
                            onLongClick = {}
                        )
                        .holdToUnlock(
                            enabled = developerModeEnabled,
                            onUnlocked = onDeveloperUnlocked,
                            onProgress = { unlockProgress = it },
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                if (unlockProgress > 0f) {
                    LinearProgressIndicator(
                        progress = { unlockProgress },
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .width(96.dp)
                            .height(3.dp),
                    )
                }
            }
        }
    }
}
