/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.presentation.picker

import android.content.ClipData
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Animation.navigateInAnimation
import com.dot.gallery.core.Constants.Animation.navigateUpAnimation
import com.dot.gallery.core.MediaState
import com.dot.gallery.core.presentation.components.StickyHeader
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.presentation.common.components.MediaComponent
import com.dot.gallery.feature_node.presentation.util.vibrate
import com.dot.gallery.ui.theme.Dimens
import com.dot.gallery.ui.theme.GalleryTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PickerActivity : ComponentActivity() {

    @OptIn(
        ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class,
        ExperimentalAnimationApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val type = intent.type
        val pickImage = type.toString().startsWith("image")
        val pickVideo = type.toString().startsWith("video")
        val pickAny = type.toString() == "*/*"
        val allowMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)

        var title = getString(R.string.select)
        title += if (allowMultiple) {
            if (pickAny) " ${getString(R.string.photos_and_videos)}"
            else if (pickImage) " ${getString(R.string.photos)}"
            else " ${getString(R.string.videos)}"
        } else {
            if (pickImage) " ${getString(R.string.photo)}"
            else if (pickVideo) " ${getString(R.string.video)}"
            else " ${getString(R.string.photos_and_videos)}"
        }
        val allowedMedia = if (pickImage) AllowedMedia.PHOTOS
        else if (pickVideo) AllowedMedia.VIDEOS
        else AllowedMedia.BOTH
        setContent {
            GalleryTheme {
                val mediaPermissions =
                    rememberMultiplePermissionsState(Constants.PERMISSIONS)
                if (!mediaPermissions.allPermissionsGranted) {
                    LaunchedEffect(Unit) {
                        mediaPermissions.launchMultiplePermissionRequest()
                    }
                }
                val navController = rememberAnimatedNavController()
                val selectionIndex = remember { mutableStateOf(0) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = title) },
                            navigationIcon = {
                                IconButton(onClick = ::finish) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = getString(R.string.close)
                                    )
                                }
                            }
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = it.calculateTopPadding()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            text = getString(R.string.picker_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        /*
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterHorizontally
                            )
                        ) {
                            TabSelector(
                                selectionIndex = selectionIndex,
                                onSelectPhotos = {
                                    navController.navigate("photos_picker") {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onSelectAlbums = {
                                    navController.navigate("albums_list") {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }*/
                        NavigationScreen(
                            allowSelection = allowMultiple,
                            paddingValues = it,
                            allowedMedia = allowedMedia,
                            navController = navController
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun NavigationScreen(
        allowedMedia: AllowedMedia,
        allowSelection: Boolean,
        paddingValues: PaddingValues,
        navController: NavHostController
    ) {
        val selectedMedia = remember { mutableStateListOf<Media>() }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedNavHost(navController = navController, startDestination = "photos_picker") {
                composable(
                    route = "photos_picker",
                    enterTransition = { navigateInAnimation },
                    exitTransition = { navigateUpAnimation },
                    popEnterTransition = { navigateInAnimation },
                    popExitTransition = { navigateUpAnimation }
                ) {
                    val vm = hiltViewModel<PickerViewModel>().apply {
                        this.allowedMedia = allowedMedia
                        init()
                    }
                    MediaScreen(
                        mediaState = vm.mediaState,
                        selectedMedia = selectedMedia,
                        allowSelection = allowSelection
                    )
                }
                composable(
                    route = "albums_list",
                    enterTransition = { navigateInAnimation },
                    exitTransition = { navigateUpAnimation },
                    popEnterTransition = { navigateInAnimation },
                    popExitTransition = { navigateUpAnimation }
                ) {

                }
                composable(
                    route = "album_{albumId}_{albumName}",
                    enterTransition = { navigateInAnimation },
                    exitTransition = { navigateUpAnimation },
                    popEnterTransition = { navigateInAnimation },
                    popExitTransition = { navigateUpAnimation },
                    arguments = listOf(
                        navArgument(name = "albumId") {
                            type = NavType.LongType
                            defaultValue = -1
                        },
                        navArgument(name = "albumName") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val argumentAlbumName = backStackEntry.arguments?.getString("albumName")
                        ?: stringResource(id = R.string.app_name)
                    val argumentAlbumId = backStackEntry.arguments?.getLong("albumId") ?: -1
                    val vm = hiltViewModel<PickerViewModel>().apply {
                        this.allowedMedia = allowedMedia
                        albumId = argumentAlbumId
                        init()
                    }
                    MediaScreen(
                        mediaState = vm.mediaState,
                        selectedMedia = selectedMedia,
                        allowSelection = allowSelection
                    )
                }
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomStart),
                visible = !allowSelection || selectedMedia.isNotEmpty(),
                enter = slideInVertically { it * 2 },
                exit = slideOutVertically { it * 2 }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "View selected")
                    val scope = rememberCoroutineScope()
                    Button(
                        onClick = {
                            scope.launch {
                                sendMediaAsResult(selectedMedia.map { it.uri })
                            }
                        },
                        enabled = selectedMedia.isNotEmpty()
                    ) {
                        if (allowSelection)
                            Text(text = "Add (${selectedMedia.size})")
                        else
                            Text(text = "Add")
                    }
                }
            }
        }
    }

    @Composable
    private fun MediaScreen(
        mediaState: StateFlow<MediaState>,
        selectedMedia: SnapshotStateList<Media>,
        allowSelection: Boolean,
    ) {
        val scope = rememberCoroutineScope()
        val stringToday = stringResource(id = R.string.header_today)
        val stringYesterday = stringResource(id = R.string.header_yesterday)
        val state by mediaState.collectAsStateWithLifecycle()
        val gridState = rememberLazyGridState()
        val isCheckVisible = rememberSaveable { mutableStateOf(allowSelection) }

        /** Glide Preloading **/
        val preloadingData = rememberGlidePreloadingData(
            data = state.media,
            preloadImageSize = Size(50f, 50f)
        ) { media: Media, requestBuilder: RequestBuilder<Drawable> ->
            requestBuilder
                .signature(MediaStoreSignature(media.mimeType, media.timestamp, media.orientation))
                .load(media.uri)
        }
        /** ************ **/
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(Dimens.Photo()),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            items(
                items = state.mappedMedia,
                key = { it.key },
                contentType = { it.key.startsWith("media_") },
                span = { item ->
                    GridItemSpan(if (item.key.isHeaderKey) maxLineSpan else 1)
                }
            ) { item ->
                when (item) {
                    is MediaItem.Header -> {
                        val isChecked = rememberSaveable { mutableStateOf(false) }
                        if (allowSelection) {
                            LaunchedEffect(selectedMedia.size) {
                                // Partial check of media items should not check the header
                                isChecked.value = selectedMedia.containsAll(item.data)
                            }
                        }
                        val title = item.text
                            .replace("Today", stringToday)
                            .replace("Yesterday", stringYesterday)
                        val view = LocalView.current
                        StickyHeader(
                            date = title,
                            showAsBig = item.key.contains("big"),
                            isCheckVisible = isCheckVisible,
                            isChecked = isChecked
                        ) {
                            if (allowSelection) {
                                view.vibrate()
                                scope.launch {
                                    isChecked.value = !isChecked.value
                                    if (isChecked.value) {
                                        val toAdd = item.data.toMutableList().apply {
                                            // Avoid media from being added twice to selection
                                            removeIf { selectedMedia.contains(it) }
                                        }
                                        selectedMedia.addAll(toAdd)
                                    } else selectedMedia.removeAll(item.data)
                                }
                            }
                        }
                    }

                    is MediaItem.MediaViewItem -> {
                        val mediaIndex = state.media.indexOf(item.media).coerceAtLeast(0)
                        val (media, preloadRequestBuilder) = preloadingData[mediaIndex]
                        val view = LocalView.current
                        val selectionState = remember { mutableStateOf(true) }
                        MediaComponent(
                            media = media,
                            selectionState = selectionState,
                            selectedMedia = selectedMedia,
                            preloadRequestBuilder = preloadRequestBuilder,
                            onItemLongClick = {
                                view.vibrate()
                                if (allowSelection) {
                                    if (selectedMedia.contains(it)) selectedMedia.remove(it)
                                    else selectedMedia.add(it)
                                } else if (!selectedMedia.contains(it) && selectedMedia.size == 1) {
                                    selectedMedia[0] = it
                                } else if (selectedMedia.isEmpty()) {
                                    selectedMedia.add(it)
                                } else {
                                    selectedMedia.remove(it)
                                }
                            },
                            onItemClick = {
                                view.vibrate()
                                if (allowSelection) {
                                    if (selectedMedia.contains(it)) selectedMedia.remove(it)
                                    else selectedMedia.add(it)
                                } else if (!selectedMedia.contains(it) && selectedMedia.size == 1) {
                                    selectedMedia[0] = it
                                } else if (selectedMedia.isEmpty()) {
                                    selectedMedia.add(it)
                                } else {
                                    selectedMedia.remove(it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TabSelector(
        selectionIndex: MutableState<Int>,
        onSelectPhotos: () -> Unit,
        onSelectAlbums: () -> Unit
    ) {
        val selectedColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        val normalColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = {
                selectionIndex.value = 0
                onSelectPhotos()
            },
            shape = RoundedCornerShape(16.dp),
            colors = if (selectionIndex.value == 0) selectedColors else normalColors
        ) {
            Text(text = stringResource(R.string.photos))
        }
        Button(
            onClick = {
                selectionIndex.value = 1
                onSelectAlbums()
            },
            shape = RoundedCornerShape(16.dp),
            colors = if (selectionIndex.value == 1) selectedColors else normalColors
        ) {
            Text(text = stringResource(R.string.nav_albums))
        }
    }

    private fun sendMediaAsResult(selectedMedia: List<Uri>) {
        val newIntent = Intent().apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            data = selectedMedia[0]
        }
        if (selectedMedia.size == 1)
            setResult(RESULT_OK, newIntent)
        else {
            val newClipData = ClipData.newUri(contentResolver, null, selectedMedia[1])
            for (nextUri in selectedMedia.stream().skip(2)) {
                newClipData.addItem(contentResolver, ClipData.Item(nextUri))
            }
            newIntent.clipData = newClipData
            setResult(RESULT_OK, newIntent)
        }
        finish()
    }
}