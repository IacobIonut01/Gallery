/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dot.gallery.BuildConfig
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.exif.CopyMediaSheet
import com.dot.gallery.feature_node.presentation.exif.MetadataEditSheet
import com.dot.gallery.feature_node.presentation.exif.MoveMediaSheet
import com.dot.gallery.feature_node.presentation.trashed.components.TrashDialog
import com.dot.gallery.feature_node.presentation.trashed.components.TrashDialogAction
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.ExifMetadata
import com.dot.gallery.feature_node.presentation.util.MapBoxURL
import com.dot.gallery.feature_node.presentation.util.connectivityState
import com.dot.gallery.feature_node.presentation.util.formattedAddress
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getLocation
import com.dot.gallery.feature_node.presentation.util.launchEditIntent
import com.dot.gallery.feature_node.presentation.util.launchMap
import com.dot.gallery.feature_node.presentation.util.launchOpenWithIntent
import com.dot.gallery.feature_node.presentation.util.launchUseAsIntent
import com.dot.gallery.feature_node.presentation.util.rememberActivityResult
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.rememberExifInterface
import com.dot.gallery.feature_node.presentation.util.rememberExifMetadata
import com.dot.gallery.feature_node.presentation.util.rememberGeocoder
import com.dot.gallery.feature_node.presentation.util.rememberMediaInfo
import com.dot.gallery.feature_node.presentation.util.shareMedia
import com.dot.gallery.ui.theme.BlackScrim
import com.dot.gallery.ui.theme.Shapes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@Composable
fun BoxScope.MediaViewBottomBar(
    showDeleteButton: Boolean = true,
    bottomSheetState: AppBottomSheetState,
    handler: MediaHandleUseCase,
    showUI: Boolean,
    paddingValues: PaddingValues,
    currentMedia: Media?,
    currentIndex: Int = 0,
    refresh: () -> Unit,
    onDeleteMedia: ((Int) -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = showUI,
        enter = enterAnimation(DEFAULT_TOP_BAR_ANIMATION_DURATION),
        exit = exitAnimation(DEFAULT_TOP_BAR_ANIMATION_DURATION),
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, BlackScrim)
                    )
                )
                .padding(
                    top = 24.dp,
                    bottom = paddingValues.calculateBottomPadding()
                )
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            currentMedia?.let {
                MediaViewActions(
                    currentIndex = currentIndex,
                    currentMedia = it,
                    handler = handler,
                    onDeleteMedia = onDeleteMedia,
                    showDeleteButton = showDeleteButton
                )
            }
        }
    }
    currentMedia?.let {
        MediaInfoBottomSheet(
            media = it,
            state = bottomSheetState,
            handler = handler,
            refresh = refresh
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoBottomSheet(
    media: Media,
    state: AppBottomSheetState,
    handler: MediaHandleUseCase,
    refresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val exifInterface = rememberExifInterface(media, true)
    val metadataState = rememberAppBottomSheetState()
    if (exifInterface != null) {
        val exifMetadata = rememberExifMetadata(media, exifInterface)
        val mediaInfoList = rememberMediaInfo(media, exifMetadata)
        if (state.isVisible) {
            ModalBottomSheet(
                onDismissRequest = {
                    scope.launch {
                        state.hide()
                        refresh()
                    }
                },
                dragHandle = { DragHandle() },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                sheetState = state.sheetState,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                BackHandler {
                    scope.launch {
                        state.hide()
                        refresh()
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    MediaViewInfoActions(media)
                    Spacer(modifier = Modifier.height(8.dp))
                    MediaInfoDateCaptionContainer(media, exifMetadata) {
                        scope.launch {
                            state.hide()
                            metadataState.show()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MediaInfoMapPreview(exifMetadata)
                    if (mediaInfoList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .height(32.dp),
                            text = stringResource(R.string.media_details),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        for (metadata in mediaInfoList) {
                            MediaInfoRow(
                                label = metadata.label,
                                content = metadata.content,
                                icon = metadata.icon
                            )
                        }
                    }
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
        if (metadataState.isVisible) {
            MetadataEditSheet(
                state = metadataState,
                media = media,
                handle = handler
            )
        }
    }
}

@Composable
fun MediaInfoDateCaptionContainer(
    media: Media,
    exifMetadata: ExifMetadata,
    onClickEditButton: () -> Unit = {}
) {
    Column {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = Shapes.large
                )
                .padding(all = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = media.timestamp.getDate(Constants.EXIF_DATE_FORMAT),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
                val defaultDesc = stringResource(R.string.image_add_description)
                val imageDesc = remember(exifMetadata) {
                    val lensDesc = exifMetadata.lensDescription
                    val imageCapt = exifMetadata.imageDescription
                    return@remember if (lensDesc != null && !imageCapt.isNullOrBlank() && imageCapt != lensDesc) {
                        "$lensDesc\n$imageCapt"
                    } else lensDesc ?: (imageCapt ?: defaultDesc)
                }
                Text(
                    text = imageDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!media.readUriOnly()) {
                IconButton(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onClick = onClickEditButton
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(id = R.string.edit_cd),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(8.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(state = rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (media.isRaw) {
                MediaInfoChip(
                    text = media.fileExtension.toUpperCase(Locale.current),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            if (exifMetadata.formattedCords != null) {
                val geocoder = rememberGeocoder()
                val clipboardManager: ClipboardManager = LocalClipboardManager.current
                var locationName by remember { mutableStateOf(exifMetadata.formattedCords!!) }
                LaunchedEffect(geocoder) {
                    geocoder?.getLocation(exifMetadata.gpsLatLong!![0], exifMetadata.gpsLatLong[1]) {address ->
                        address?.let {
                            val addressName = it.formattedAddress
                            if (addressName.isNotEmpty()) {
                                locationName = addressName
                            }
                        }
                    }
                }
                MediaInfoChip(
                    text = stringResource(R.string.location_chip, locationName),
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(exifMetadata.formattedCords!!))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaInfoChip(
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    outlineInLightTheme: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    Text(
        modifier = Modifier
            .background(
                color = containerColor,
                shape = Shapes.extraLarge
            )
            .then(
                if (!isSystemInDarkTheme() && outlineInLightTheme) Modifier.border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = Shapes.extraLarge
                ) else Modifier
            )
            .clip(Shapes.extraLarge)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = contentColor
    )
}

@Suppress("KotlinConstantConditions")
@OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalGlideComposeApi::class
)
@Composable
fun MediaInfoMapPreview(exifMetadata: ExifMetadata) {
    if (exifMetadata.gpsLatLong != null) {
        val context = LocalContext.current
        val lat = exifMetadata.gpsLatLong[0]
        val long = exifMetadata.gpsLatLong[1]
        val connection by connectivityState()
        if (connection.isConnected() && BuildConfig.MAPS_TOKEN != "DEBUG") {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(Shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                GlideImage(
                    model = MapBoxURL(
                        latitude = lat,
                        longitude = long,
                        darkTheme = isSystemInDarkTheme()
                    ),
                    contentScale = ContentScale.FillWidth,
                    contentDescription = stringResource(R.string.location_map_cd),
                    modifier = Modifier
                        .clip(Shapes.large)
                        .fillMaxWidth()
                        .aspectRatio(1.78f)
                        .clickable { context.launchMap(lat, long) }
                )
                Icon(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MediaViewInfoActions(
    media: Media
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Share Component
        ShareButton(media, followTheme = true)
        // Use as or Open With
        OpenAsButton(media, followTheme = true)
        // Copy
        CopyButton(media, followTheme = true)
        // Move
        MoveButton(media, followTheme = true)
        // Edit
        EditButton(media, followTheme = true)
    }
}

@Composable
private fun MediaViewActions(
    currentIndex: Int,
    currentMedia: Media,
    handler: MediaHandleUseCase,
    onDeleteMedia: ((Int) -> Unit)?,
    showDeleteButton: Boolean
) {
    // Share Component
    ShareButton(currentMedia)
    // Favorite Component
    FavoriteButton(currentMedia, handler)
    // Edit
    EditButton(currentMedia)
    // Trash Component
    if (showDeleteButton) {
        TrashButton(currentIndex, currentMedia, handler, false, onDeleteMedia)
    }
}

@Composable
private fun CopyButton(
    media: Media,
    followTheme: Boolean = false
) {
    val copySheetState = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    BottomBarColumn(
        currentMedia = media,
        imageVector = Icons.Outlined.CopyAll,
        followTheme = followTheme,
        title = stringResource(R.string.copy)
    ) {
        scope.launch {
            copySheetState.show()
        }
    }

    CopyMediaSheet(
        sheetState = copySheetState,
        mediaList = listOf(media),
        onFinish = {  }
    )
}

@Composable
private fun MoveButton(
    media: Media,
    followTheme: Boolean = false
) {
    val moveSheetState = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    BottomBarColumn(
        currentMedia = media,
        imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
        followTheme = followTheme,
        title = stringResource(R.string.move)
    ) {
        scope.launch {
            moveSheetState.show()
        }
    }

    MoveMediaSheet(
        sheetState = moveSheetState,
        mediaList = listOf(media),
        onFinish = {  }
    )
}

@Composable
private fun ShareButton(
    media: Media,
    followTheme: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    BottomBarColumn(
        currentMedia = media,
        imageVector = Icons.Outlined.Share,
        followTheme = followTheme,
        title = stringResource(R.string.share)
    ) {
        scope.launch {
            context.shareMedia(media = it)
        }
    }
}

@Composable
private fun FavoriteButton(
    media: Media,
    handler: MediaHandleUseCase,
    followTheme: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val result = rememberActivityResult()
    val favoriteIcon by remember(media) {
        mutableStateOf(
            if (media.isFavorite)
                Icons.Filled.Favorite
            else Icons.Outlined.FavoriteBorder
        )
    }
    if (!media.readUriOnly()) {
        BottomBarColumn(
            currentMedia = media,
            imageVector = favoriteIcon,
            followTheme = followTheme,
            title = stringResource(id = R.string.favorites)
        ) {
            scope.launch {
                handler.toggleFavorite(result = result, arrayListOf(it), it.favorite != 1)
            }
        }
    }
}

@Composable
private fun EditButton(
    media: Media,
    followTheme: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BottomBarColumn(
        currentMedia = media,
        imageVector = Icons.Outlined.Edit,
        followTheme = followTheme,
        title = stringResource(R.string.edit)
    ) {
        scope.launch { context.launchEditIntent(it) }
    }
}

@Composable
private fun OpenAsButton(
    media: Media,
    followTheme: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (media.isVideo) {
        BottomBarColumn(
            currentMedia = media,
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            followTheme = followTheme,
            title = stringResource(R.string.open_with)
        ) {
            scope.launch { context.launchOpenWithIntent(it) }
        }
    } else {
        BottomBarColumn(
            currentMedia = media,
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            followTheme = followTheme,
            title = stringResource(R.string.use_as)
        ) {
            scope.launch { context.launchUseAsIntent(it) }
        }
    }
}

@Composable
private fun TrashButton(
    index: Int,
    media: Media,
    handler: MediaHandleUseCase,
    followTheme: Boolean = false,
    onDeleteMedia: ((Int) -> Unit)?
) {
    val state = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    val result = rememberActivityResult {
        scope.launch {
            state.hide()
            onDeleteMedia?.invoke(index)
        }
    }
    BottomBarColumn(
        currentMedia = media,
        imageVector = Icons.Outlined.DeleteOutline,
        followTheme = followTheme,
        title = stringResource(id = R.string.trash)
    ) {
        scope.launch {
            state.show()
        }
    }

    TrashDialog(
        appBottomSheetState = state,
        data = listOf(media),
        action = TrashDialogAction.TRASH
    ) {
        handler.trashMedia(result, it, true)
    }
}

@Composable
fun BottomBarColumn(
    currentMedia: Media?,
    imageVector: ImageVector,
    title: String,
    followTheme: Boolean = false,
    onItemClick: (Media) -> Unit
) {
    val tintColor = if (followTheme) MaterialTheme.colorScheme.onSurface else Color.White
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .defaultMinSize(
                minWidth = 90.dp,
                minHeight = 80.dp
            )
            .clickable {
                currentMedia?.let {
                    onItemClick.invoke(it)
                }
            }
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = imageVector,
            colorFilter = ColorFilter.tint(tintColor),
            contentDescription = title,
            modifier = Modifier
                .height(32.dp)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = title,
            modifier = Modifier,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = tintColor,
            textAlign = TextAlign.Center
        )
    }
}