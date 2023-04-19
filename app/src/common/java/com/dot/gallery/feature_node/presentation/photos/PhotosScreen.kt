/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.photos

import android.Manifest
import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.LoadingMedia
import com.dot.gallery.core.presentation.components.media.MediaComponent
import com.dot.gallery.core.presentation.components.util.StickyHeaderGrid
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.presentation.MediaViewModel
import com.dot.gallery.feature_node.presentation.photos.components.StickyHeader
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getDateExt
import com.dot.gallery.feature_node.presentation.util.getDateHeader
import com.dot.gallery.feature_node.presentation.util.getMonth
import com.dot.gallery.feature_node.presentation.util.shareMedia
import com.dot.gallery.ui.theme.Dimens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(
    ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class
)
@Composable
fun PhotosScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    albumId: Long = -1L,
    albumName: String = stringResource(id = R.string.app_name),
    viewModel: MediaViewModel,
) {

    /** STRING BLOCK **/
    val stringToday = stringResource(id = R.string.header_today)
    val stringYesterday = stringResource(id = R.string.header_yesterday)
    val requestPermission = stringResource(R.string.request_permissions)
    val shareMedia = stringResource(id = R.string.share_media)
    val trashMedia = stringResource(R.string.trash)
    /** ************ **/

    /** Permission Handling BLOCK **/
    val mediaPermissions = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
    )
        }
    ) {
        viewModel.launchInPhotosScreen()
    }
    /** Trigger viewModel launch after permission is granted */
    var firstStart by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(mediaPermissions) {
        if (firstStart) {
            firstStart = false
        }
        if (!mediaPermissions.allPermissionsGranted){
            firstStart = true
        }
    }
    if (!mediaPermissions.allPermissionsGranted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = mediaPermissions::launchMultiplePermissionRequest) {
                Text(text = requestPermission)
            }
        }
    }
    /** ************ **/
    else {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        /** STATES BLOCK **/
        val gridState = rememberLazyGridState()
        val state by remember { viewModel.photoState }
        val selectionState = remember { viewModel.multiSelectState }
        val selectedMedia = remember { viewModel.selectedPhotoState }
        /** ************ **/

        val handler = viewModel.handler

        /** Glide Preloading **/
        val preloadingData = rememberGlidePreloadingData(
            data = state.media,
            numberOfItemsToPreload = sqrt(state.media.size.toDouble()).roundToInt(),
            preloadImageSize = Size(100f, 100f)
        ) { media: Media, requestBuilder: RequestBuilder<Drawable> ->
            requestBuilder.load(media.uri)
                .signature(
                    MediaStoreSignature(
                        media.mimeType,
                        media.timestamp,
                        media.orientation
                    )
                )
        }
        /** ************ **/

        /** Selection state handling **/
        val clearSelection = {
            selectionState.value = false
            selectedMedia.clear()
        }

        val result = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
            onResult = {
                if (it.resultCode == Activity.RESULT_OK) clearSelection()
            }
        )

        BackHandler(enabled = selectionState.value) { clearSelection() }
        /** ************  **/

        /**
         * This block requires recomputing on state change
         * avoid 'remember'
         */
        val sortedAscendingMedia = state.media.sortedBy { it.timestamp }
        val startDate = try {
            sortedAscendingMedia.first().timestamp.getDateExt()
        } catch (e: NoSuchElementException) {
            null
        }
        val endDate = try {
            sortedAscendingMedia.last().timestamp.getDateExt()
        } catch (e: NoSuchElementException) {
            null
        }
        val subtitle = if (albumId != -1L && startDate != null && endDate != null) getDateHeader(
            startDate, endDate
        ) else null
        val mappedData = ArrayList<MediaItem>()
        val monthHeaderList = ArrayList<String>()
        state.media.groupBy {
            it.timestamp.getDate(
                stringToday = stringToday,
                stringYesterday = stringYesterday
            )
        }.forEach { (date, data) ->
            val month = getMonth(date)
            if (month.isNotEmpty() && !monthHeaderList.contains(month)) {
                monthHeaderList.add(month)
                mappedData.add(MediaItem.Header("header_big_$month", month))
            }
            mappedData.add(MediaItem.Header("header_$date", date))
            for (media in data) {
                mappedData.add(MediaItem.MediaViewItem.Loaded("media_${media.id}", media))
            }
        }
        /** ************ **/

        /**
         * Remember last known header item
         */
        val stickyHeaderLastItem = remember {
            mutableStateOf<String?>(null)
        }

        val stickyHeaderItem by remember(state.media) {
            derivedStateOf {
                val firstIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                val item = firstIndex?.let(mappedData::getOrNull)
                stickyHeaderLastItem.apply {
                    if (item != null && item is MediaItem.Header) {
                        value = item.key.replace("header_", "")
                    }
                }.value
            }
        }

        var expandedDropDown by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            val toolbarTitle = if (selectionState.value) stringResource(
                                R.string.selected_s,
                                selectedMedia.size
                            ) else albumName
                            Text(
                                text = toolbarTitle,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            if (!subtitle.isNullOrEmpty()) {
                                Text(
                                    modifier = Modifier,
                                    text = subtitle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        val onClick: () -> Unit =
                            if (albumId != -1L && !selectionState.value)
                                navController::navigateUp
                            else
                                clearSelection
                        val icon = if (albumId != -1L && !selectionState.value)
                                Icons.Default.ArrowBack
                            else
                                Icons.Default.Close
                        if (albumId != -1L || selectionState.value) {
                            IconButton(onClick = onClick) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stringResource(R.string.back_cd)
                                )
                            }
                        }
                    },
                    actions = {
                        AnimatedVisibility(
                            visible = selectionState.value,
                            enter = enterAnimation,
                            exit = exitAnimation
                        ) {
                            Row {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            context.shareMedia(selectedMedia)
                                            selectionState.value = false
                                            selectedMedia.clear()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = shareMedia
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch { handler.toggleFavorite(result, selectedMedia) }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = trashMedia
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch { handler.trashMedia(result, selectedMedia) }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = trashMedia
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { expandedDropDown = !expandedDropDown }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.drop_down_cd)
                            )
                        }
                        DropdownMenu(
                            modifier = Modifier,
                            expanded = expandedDropDown,
                            onDismissRequest = { expandedDropDown = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (selectionState.value)
                                            stringResource(R.string.unselect_all)
                                        else
                                            stringResource(R.string.select_all)
                                    )
                                },
                                onClick = {
                                    selectionState.value = !selectionState.value
                                    if (selectionState.value)
                                        selectedMedia.addAll(state.media)
                                    else
                                        selectedMedia.clear()
                                    expandedDropDown = false
                                },
                            )
                            if (albumId != -1L) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(R.string.move_album_to_trash)) },
                                    onClick = {
                                        viewModel.viewModelScope.launch {
                                            viewModel.handler.trashMedia(
                                                result = result,
                                                mediaList = state.media,
                                                trash = true
                                            )
                                            navController.navigateUp()
                                        }
                                        expandedDropDown = false
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.favorites)) },
                                onClick = {
                                    navController.navigate(Screen.FavoriteScreen.route)
                                    expandedDropDown = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.trash)) },
                                onClick = {
                                    navController.navigate(Screen.TrashedScreen.route)
                                    expandedDropDown = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.settings_title)) },
                                onClick = {
                                    navController.navigate(Screen.SettingsScreen.route)
                                    expandedDropDown = false
                                }
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { it ->
            StickyHeaderGrid(
                modifier = Modifier.fillMaxSize(),
                lazyState = gridState,
                headerMatcher = { item -> item.key.isHeaderKey },
                stickyHeader = {
                    if (state.media.isNotEmpty()) {
                        stickyHeaderItem?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                // 3.dp is the elevation the LargeTopAppBar use
                                                MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 24.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(Dimens.Photo()),
                    contentPadding = PaddingValues(
                        top = it.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(
                        items = mappedData,
                        key = { it.key },
                        span = { item ->
                            GridItemSpan(if (item.key.isHeaderKey) maxLineSpan else 1)
                        }
                    ) { item ->
                        when (item) {
                            is MediaItem.Header -> StickyHeader(date = item.text, item.key.contains("big"))
                            is MediaItem.MediaViewItem -> {
                                val (media, preloadRequestBuilder) = preloadingData[state.media.indexOf(
                                    item.media
                                )]
                                MediaComponent(
                                    media = media,
                                    selectionState = selectionState,
                                    isSelected = mutableStateOf(selectedMedia.find { it.id == media.id } != null),
                                    preloadRequestBuilder = preloadRequestBuilder,
                                    onItemLongClick = {
                                        viewModel.toggleSelection(state.media.indexOf(it))
                                    },
                                    onItemClick = {
                                        if (selectionState.value) {
                                            viewModel.toggleSelection(state.media.indexOf(it))
                                        } else {
                                            navController.navigate(
                                                Screen.MediaViewScreen.route +
                                                        "?mediaId=${it.id}&albumId=${albumId}"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        /** Error State Handling Block **/
        if (state.isLoading) {
            LoadingMedia(modifier = Modifier.fillMaxSize())
        }
        if (state.media.isEmpty()) {
            EmptyMedia(modifier = Modifier.fillMaxSize())
        }
        if (state.error.isNotEmpty()) {
            Error(errorMessage = state.error)
        }
        /** ************ **/
    }
}