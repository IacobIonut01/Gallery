/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.customalbums.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.util.AutoResizeText
import com.dot.gallery.core.presentation.components.util.FontSizeRange
import com.dot.gallery.feature_node.domain.model.CustomAlbum
import com.dot.gallery.feature_node.presentation.common.components.OptionItem
import com.dot.gallery.feature_node.presentation.common.components.OptionSheet
import com.dot.gallery.feature_node.presentation.customalbums.dialogs.DeleteCustomAlbumDialog
import com.dot.gallery.feature_node.presentation.util.FeedbackManager.Companion.rememberFeedbackManager
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.ui.theme.Shapes
import kotlinx.coroutines.launch

@Composable
fun CustomAlbumComponent(
    modifier: Modifier = Modifier,
    album: CustomAlbum,
    isEnabled: Boolean = true,
    onItemClick: (CustomAlbum) -> Unit,
    onTogglePinClick: ((CustomAlbum) -> Unit)? = null,
    onDeleteCustomAlbum: ((CustomAlbum) -> Unit)? = null,
    onToggleIgnoreClick: ((CustomAlbum) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val appBottomSheetState = rememberAppBottomSheetState()
    Column(
        modifier = modifier
            .alpha(if (isEnabled) 1f else 0.4f)
            .padding(horizontal = 8.dp),
    ) {
        if (onTogglePinClick != null) {
            val pinTitle = stringResource(R.string.pin)
            val deleteTitle = stringResource(R.string.customalbum_delete)
            val ignoredTitle = stringResource(id = R.string.add_to_ignored)
            val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
            val onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer

            var showDialog by remember { mutableStateOf(false) }
            if (showDialog) {
                DeleteCustomAlbumDialog(
                    album,
                    onConfirmation = {
                        scope.launch {
                            if (onDeleteCustomAlbum != null) {
                                onDeleteCustomAlbum(album)
                                appBottomSheetState.hide()
                            }
                        }
                        showDialog = false
                    },
                    onDismiss = {
                        showDialog = false
                    }
                )
            }

            val optionList = remember {
                mutableListOf(
                    OptionItem(
                        text = pinTitle,
                        containerColor = tertiaryContainer,
                        contentColor = onTertiaryContainer,
                        onClick = {
                            scope.launch {
                                appBottomSheetState.hide()
                                onTogglePinClick(album)
                            }
                        }
                    ),
                    OptionItem(
                        text = deleteTitle,
                        onClick = {
                            showDialog = true
                        }
                    )
                )
            }
            LaunchedEffect(onToggleIgnoreClick) {
                if (onToggleIgnoreClick != null) {
                    optionList.add(
                        OptionItem(
                            text = ignoredTitle,
                            onClick = {
                                scope.launch {
                                    appBottomSheetState.hide()
                                    onToggleIgnoreClick(album)
                                }
                            }
                        )
                    )
                }
            }

            OptionSheet(
                state = appBottomSheetState,
                optionList = arrayOf(optionList),
                headerContent = {
                    AsyncImage(
                        modifier = Modifier
                            .size(98.dp)
                            .clip(Shapes.large),
                        contentScale = ContentScale.Crop,
                        model = null,
                        contentDescription = album.label
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing
                                )
                            ) {
                                append(album.label)
                            }
                            append("\n")
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                    letterSpacing = MaterialTheme.typography.bodyMedium.letterSpacing
                                )
                            ) {
                                append(stringResource(R.string.s_items, album.count))
                            }
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    )
                }
            )
        }
        Box(
            modifier = Modifier
                .aspectRatio(1f)
        ) {
            CustomAlbumImage(
                album = album,
                isEnabled = isEnabled,
                onItemClick = onItemClick,
                onItemLongClick = if (onTogglePinClick != null) {
                    {
                        scope.launch {
                            appBottomSheetState.show()
                        }
                    }
                } else null
            )
        }
        AutoResizeText(
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
            text = album.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            fontSizeRange = FontSizeRange(
                min = 10.sp,
                max = 16.sp
            )
        )
        if (album.count > 0) {
            AutoResizeText(
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                text = pluralStringResource(
                    id = R.plurals.item_count,
                    count = album.count.toInt(),
                    album.count
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium,
                fontSizeRange = FontSizeRange(
                    min = 6.sp,
                    max = 12.sp
                )
            )
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomAlbumImage(
    album: CustomAlbum,
    isEnabled: Boolean,
    onItemClick: (CustomAlbum) -> Unit,
    onItemLongClick: ((CustomAlbum) -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val radius = if (isPressed.value) 32.dp else 16.dp
    val cornerRadius by animateDpAsState(targetValue = radius, label = "cornerRadius")
    val feedbackManager = rememberFeedbackManager()
    if (album.id == -200L && album.count == 0L) {
        Icon(
            imageVector = Icons.Outlined.AddCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .alpha(0.8f)
                .clip(RoundedCornerShape(cornerRadius))
                .combinedClickable(
                    enabled = isEnabled,
                    interactionSource = interactionSource,
                    indication = rememberRipple(),
                    onClick = { onItemClick(album) },
                    onLongClick = {
                        onItemLongClick?.let {
                            feedbackManager.vibrate()
                            it(album)
                        }
                    }
                )
                .padding(48.dp)
        )
    } else {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .combinedClickable(
                    enabled = isEnabled,
                    interactionSource = interactionSource,
                    indication = rememberRipple(),
                    onClick = { onItemClick(album) },
                    onLongClick = {
                        onItemLongClick?.let {
                            feedbackManager.vibrate()
                            it(album)
                        }
                    }
                ),
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = album.label,
            contentScale = ContentScale.Crop,
        )
    }
}