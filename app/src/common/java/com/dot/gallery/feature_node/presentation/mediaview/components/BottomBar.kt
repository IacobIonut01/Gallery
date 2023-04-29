/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components

import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.EXIF_DATE_FORMAT
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.util.ConnectionState
import com.dot.gallery.feature_node.presentation.util.ExifMetadata
import com.dot.gallery.feature_node.presentation.util.MapBoxURL
import com.dot.gallery.feature_node.presentation.util.connectivityState
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getExifInterface
import com.dot.gallery.feature_node.presentation.util.launchMap
import com.dot.gallery.feature_node.presentation.util.shareMedia
import com.dot.gallery.ui.theme.Black40P
import com.dot.gallery.ui.theme.Shapes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalGlideComposeApi::class, ExperimentalCoroutinesApi::class
)
@Composable
fun BoxScope.MediaViewBottomBar(
    showDeleteButton: Boolean = true,
    handler: MediaHandleUseCase,
    showUI: Boolean,
    paddingValues: PaddingValues,
    currentMedia: Media?,
    currentIndex: Int = 0,
    result: ActivityResultLauncher<IntentSenderRequest>? = null,
    onDeleteMedia: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    var favoriteIcon by remember {
        mutableStateOf(
            if (currentMedia != null && currentMedia.favorite == 1)
                Icons.Filled.Favorite
            else Icons.Outlined.FavoriteBorder
        )
    }
    val scope = rememberCoroutineScope()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(currentMedia) {
        favoriteIcon = if (currentMedia != null && currentMedia.favorite == 1)
            Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    }
    AnimatedVisibility(
        visible = showUI,
        enter = Constants.Animation.enterAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
        exit = Constants.Animation.exitAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Black40P)
                    )
                )
                .padding(
                    top = 24.dp,
                    bottom = paddingValues.calculateBottomPadding()
                )
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Share Component
            BottomBarColumn(
                currentMedia = currentMedia,
                imageVector = Icons.Outlined.Share,
                title = stringResource(R.string.share)
            ) {
                context.shareMedia(media = it)
            }
            // Favorite Component
            BottomBarColumn(
                currentMedia = currentMedia,
                imageVector = favoriteIcon,
                title = stringResource(id = R.string.favorites)
            ) {
                result?.let { result ->
                    scope.launch {
                        handler.toggleFavorite(result = result, arrayListOf(it), it.favorite != 1)
                    }
                }
            }
            if (showDeleteButton) {
                // Trash Component
                BottomBarColumn(
                    currentMedia = currentMedia,
                    imageVector = Icons.Outlined.DeleteOutline,
                    title = stringResource(id = R.string.trash)
                ) {
                    result?.let { result ->
                        scope.launch {
                            handler.trashMedia(result = result, arrayListOf(it))
                            onDeleteMedia?.invoke(currentIndex)
                        }
                    }
                }
            }
            // Info Component
            if (currentMedia != null) {
                BottomBarColumn(
                    currentMedia = currentMedia,
                    imageVector = Icons.Outlined.Info,
                    title = stringResource(R.string.info)
                ) {
                    openBottomSheet = true
                }
            }
        }
    }
    if (currentMedia != null) {
        val metadataList = remember(currentMedia) { currentMedia.retrieveMetadata(context) }
        val exifMetadata = remember(currentMedia) {
            ExifMetadata(
                getExifInterface(
                    context = context,
                    uri = MediaStore.setRequireOriginal(currentMedia.uri)
                )
            )
        }
        if (openBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    openBottomSheet = false
                    scope.launch {
                        bottomSheetState.hide()
                    }
                },
                modifier = Modifier
                    .absoluteOffset(y = paddingValues.calculateBottomPadding()),
                sheetState = bottomSheetState,
                dragHandle = {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 11.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Box(Modifier.size(width = 32.dp, height = 4.dp))
                    }
                }
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Share Component
                        BottomBarColumn(
                            currentMedia = currentMedia,
                            imageVector = Icons.Outlined.Share,
                            followTheme = true,
                            title = stringResource(R.string.share)
                        ) {
                            context.shareMedia(media = it)
                        }
                        // Favorite Component
                        BottomBarColumn(
                            currentMedia = currentMedia,
                            imageVector = favoriteIcon,
                            followTheme = true,
                            title = stringResource(id = R.string.favorites)
                        ) {
                            result?.let { result ->
                                scope.launch {
                                    handler.toggleFavorite(
                                        result = result,
                                        arrayListOf(it),
                                        it.favorite != 1
                                    )
                                }
                            }
                        }
                        if (showDeleteButton) {
                            // Trash Component
                            BottomBarColumn(
                                currentMedia = currentMedia,
                                imageVector = Icons.Outlined.DeleteOutline,
                                followTheme = true,
                                title = stringResource(id = R.string.trash)
                            ) {
                                result?.let { result ->
                                    scope.launch {
                                        handler.trashMedia(result = result, arrayListOf(it))
                                        onDeleteMedia?.invoke(currentIndex)
                                    }
                                }
                            }
                        }
                    }
                    Divider(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    Text(
                        text = currentMedia.timestamp.getDate(EXIF_DATE_FORMAT),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                    if (exifMetadata.gpsLatLong != null) {
                        val lat = exifMetadata.gpsLatLong[0]
                        val long = exifMetadata.gpsLatLong[1]
                        val connection by connectivityState()
                        val isConnected = connection == ConnectionState.Available
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .clip(Shapes.medium)
                                .combinedClickable(
                                    onClick = {
                                        context.launchMap(lat, long)
                                    }
                                ),
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.location),
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = stringResource(R.string.location_cd)
                                )
                            },
                            overlineContent = if (isConnected) { {} } else null,
                            supportingContent = {
                                if (isConnected) {
                                    Row(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .fillMaxWidth()
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
                                                .size(width = 247.5.dp, height = 165.dp)
                                                .clip(Shapes.large)
                                                .border(
                                                    width = 0.5.dp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    Shapes.large
                                                )
                                        )
                                        Image(
                                            imageVector = Icons.Outlined.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(start = 32.dp),
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                                        )
                                    }
                                } else {
                                    Text(text = "$lat, $long")
                                }
                            }
                        )
                    }
                    for (metadata in metadataList) {
                        MediaInfoRow(
                            label = metadata.label,
                            content = metadata.content,
                            icon = metadata.icon
                        )
                    }
                    Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                }
                BackHandler {
                    openBottomSheet = false
                    scope.launch {
                        bottomSheetState.hide()
                    }
                }
            }
        }
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
            .height(80.dp)
            .width(90.dp)
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