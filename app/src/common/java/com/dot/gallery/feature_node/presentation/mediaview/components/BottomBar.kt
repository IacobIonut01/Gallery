/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components

import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.util.shareMedia
import com.dot.gallery.ui.theme.Black40P
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val scrollState = rememberScrollState()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(currentMedia) {
        favoriteIcon = if (currentMedia != null && currentMedia.favorite == 1)
            Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    }
    BackHandler(enabled = bottomSheetState.isVisible) {
        openBottomSheet = false
        scope.launch {
            bottomSheetState.hide()
        }
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
        if (openBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { openBottomSheet = false },
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.padding(horizontal = 8.dp),
                sheetState = bottomSheetState
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                ) {
                    for (metadata in metadataList) {
                        MediaInfoRow(
                            label = metadata.label,
                            content = metadata.content,
                            icon = metadata.icon
                        )
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
    onItemClick: (Media) -> Unit
) {
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
            colorFilter = ColorFilter.tint(Color.White),
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
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}