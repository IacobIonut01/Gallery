/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Target.TARGET_FAVORITES
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.trashed.components.TrashDialog
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.rememberIsMediaManager
import com.dot.gallery.feature_node.presentation.util.shareMedia
import com.dot.gallery.ui.theme.Shapes
import kotlinx.coroutines.launch

@Composable
fun SelectionSheet(
    modifier: Modifier = Modifier,
    target: String?,
    selectedMedia: SnapshotStateList<Media>,
    selectionState: MutableState<Boolean>,
    handler: MediaHandleUseCase
) {
    fun clearSelection() {
        selectedMedia.clear()
        selectionState.value = false
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appSheetState = rememberAppBottomSheetState()
    val isMediaManager = rememberIsMediaManager()
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                clearSelection()
                if (appSheetState.isVisible) {
                    scope.launch {
                        appSheetState.hide()
                    }
                }
            }
        }
    )
    val config = LocalConfiguration.current
    val fullSize = remember(config) {
        config.orientation == Configuration.ORIENTATION_PORTRAIT
    }
    val sizeModifier = if (fullSize) Modifier.fillMaxWidth()
    else Modifier.wrapContentWidth()
    AnimatedVisibility(
        modifier = modifier,
        visible = selectionState.value,
        enter = slideInVertically { it * 2 },
        exit = slideOutVertically { it * 2 }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .then(sizeModifier)
                .wrapContentHeight()
                .clip(Shapes.extraLarge)
                .shadow(
                    elevation = 4.dp,
                    shape = Shapes.extraLarge
                )
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .then(sizeModifier)
                    .height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { clearSelection() },
                    modifier = Modifier.size(24.dp),
                ) {
                    Image(
                        imageVector = Icons.Outlined.Close,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        contentDescription = stringResource(R.string.selection_dialog_close_cd)
                    )
                }
                Text(
                    text = stringResource(R.string.selected_s, selectedMedia.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Row(
                modifier = Modifier
                    .then(sizeModifier)
                    .horizontalScroll(rememberScrollState())
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = Shapes.large
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Share Component
                SelectionBarColumn(
                    selectedMedia = selectedMedia,
                    imageVector = Icons.Outlined.Share,
                    title = stringResource(R.string.share)
                ) {
                    context.shareMedia(it)
                }
                val favoriteTitle =
                    if (target == TARGET_FAVORITES) stringResource(id = R.string.remove_selected)
                    else stringResource(id = R.string.favorites)
                // Favorite Component
                SelectionBarColumn(
                    selectedMedia = selectedMedia,
                    imageVector = Icons.Outlined.FavoriteBorder,
                    title = favoriteTitle
                ) {
                    scope.launch {
                        handler.toggleFavorite(result = result, it)
                    }
                }
                // Trash Component
                SelectionBarColumn(
                    selectedMedia = selectedMedia,
                    imageVector = Icons.Outlined.DeleteOutline,
                    title = stringResource(id = R.string.trash)
                ) {
                    scope.launch {
                        if (isMediaManager) {
                            appSheetState.show()
                        } else {
                            handler.trashMedia(result = result, it)
                        }
                    }
                }
            }
        }
    }

    TrashDialog(
        appBottomSheetState = appSheetState,
        data = selectedMedia,
        defaultText = {
            stringResource(R.string.trash_dialog_title, it)
        },
        confirmedText = {
            stringResource(R.string.trash_dialog_title_confirmation, it)
        },
        image = Icons.Outlined.DeleteOutline,
        onConfirm = {
            handler.trashMedia(result, it, true)
        }
    )
}

@Composable
private fun SelectionBarColumn(
    selectedMedia: SnapshotStateList<Media>,
    imageVector: ImageVector,
    title: String,
    onItemClick: (List<Media>) -> Unit
) {
    val tintColor = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .height(80.dp)
            .width(90.dp)
            .clickable {
                onItemClick.invoke(selectedMedia)
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