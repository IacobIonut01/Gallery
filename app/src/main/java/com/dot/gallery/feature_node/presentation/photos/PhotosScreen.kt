package com.dot.gallery.feature_node.presentation.photos

import android.Manifest
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.MediaComponent
import com.dot.gallery.core.presentation.components.util.StickyHeaderGrid
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.presentation.photos.components.StickyHeader
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.getDateExt
import com.dot.gallery.feature_node.presentation.util.getDateHeader
import com.dot.gallery.ui.theme.Dimens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    albumId: Long = -1L,
    albumName: String = stringResource(id = R.string.app_name),
    viewModel: PhotosViewModel = hiltViewModel(),
) {
    val firstStart = remember {
        mutableStateOf(true)
    }
    if (firstStart.value) {
        viewModel.albumId = albumId
        firstStart.value = false
    }
    val mediaPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    )
    if (!mediaPermissions.allPermissionsGranted) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { mediaPermissions.launchMultiplePermissionRequest() }
            ) {
                Text(text = stringResource(R.string.request_permissions))
            }
        }
    } else {
        val gridState = rememberLazyGridState()
        val scrollBehavior =
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        val state by remember {
            viewModel.photoState
        }
        val preloadingData = rememberGlidePreloadingData(
            data = state.media,
            numberOfItemsToPreload = sqrt(state.media.size.toDouble()).roundToInt(),
            preloadImageSize = Size(50f, 50f)
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

        val sortedAscendingMedia = remember {
            state.media.sortedBy { it.timestamp }
        }
        val startDate = remember {
            try {
                sortedAscendingMedia.first().timestamp.getDateExt()
            } catch (e: NoSuchElementException) {
                null
            }
        }
        val endDate = remember {
            try {
                sortedAscendingMedia.last().timestamp.getDateExt()
            } catch (e: NoSuchElementException) {
                null
            }
        }
        val subtitle = remember {
            if (albumId != -1L && startDate != null && endDate != null) getDateHeader(
                startDate, endDate
            ) else null
        }
        val stringToday = stringResource(id = R.string.header_today)
        val stringYesterday = stringResource(id = R.string.header_yesterday)
        val mappedData = remember {
            val items = ArrayList<MediaItem>()
            state.media.groupBy {
                it.timestamp.getDate(
                    stringToday = stringToday,
                    stringYesterday = stringYesterday
                )
            }.forEach { (date, data) ->
                items.add(MediaItem.Header("header_$date", date))
                for (media in data) {
                    items.add(MediaItem.MediaViewItem.Loaded("media_${media.id}", media))
                }
            }
            items
        }

        val stickyHeaderItem by remember(state.media) {
            derivedStateOf {
                val firstIndex = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                val item = firstIndex?.let(state.media::getOrNull)
                item?.timestamp?.getDate(
                    stringToday = stringToday,
                    stringYesterday = stringYesterday
                )
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = albumName,
                            )
                            if (!subtitle.isNullOrEmpty()) {
                                Text(
                                    modifier = Modifier,
                                    text = subtitle.uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (albumId != -1L) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { it ->
            StickyHeaderGrid(
                modifier = Modifier.fillMaxSize(),
                lazyState = gridState,
                paddingValues = it,
                headerMatcher = { item -> item.key.isHeaderKey },
                stickyHeader = {
                    stickyHeaderItem?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.4f
                                            ), Color.Transparent
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                                .fillMaxWidth()
                        )
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
                        },
                        itemContent = { item ->
                            when (item) {
                                is MediaItem.Header -> StickyHeader(date = item.text)
                                is MediaItem.MediaViewItem -> {
                                    val (media, preloadRequestBuilder) = preloadingData[state.media.indexOf(
                                        item.media
                                    )]
                                    MediaComponent(media = media, preloadRequestBuilder) {
                                        navController.navigate(Screen.MediaViewScreen.route + "?mediaId=${media.id}&albumId=${albumId}")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        /*
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(Dimens.Photo()),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            header {
                Toolbar(
                    navController = navController,
                    text = albumName,
                    subtitle = if (albumId != -1L && startDate != null && endDate != null) getDateHeader(
                        startDate,
                        endDate
                    ) else null
                )
            }
            groupedMedia.forEach { (date, data) ->
                header {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 24.dp
                            )
                            .fillMaxWidth()
                    )
                }
                items(data.size) { index ->
                    val preloadingData = rememberGlidePreloadingData(
                        data = data,
                        numberOfItemsToPreload = sqrt(data.size.toDouble()).roundToInt(),
                        preloadImageSize = Size(50f, 50f)
                    ) { item: Media, requestBuilder: RequestBuilder<Drawable> ->
                        requestBuilder.load(item.uri)
                            .signature(
                                MediaStoreSignature(
                                    item.mimeType,
                                    item.timestamp,
                                    item.orientation
                                )
                            )
                    }
                    val (media, preloadRequestBuilder) = preloadingData[index]
                    MediaComponent(media = media, preloadRequestBuilder) {
                        navController.navigate(Screen.MediaViewScreen.route + "?mediaId=${media.id}&albumId=${albumId}")
                    }
                }
            }
        }
         */
        if (state.media.isEmpty()) {
            Text(
                text = "Is Empty",
                modifier = Modifier
                    .fillMaxSize()
            )
            viewModel.viewModelScope.launch {
                viewModel.getMedia(albumId)
            }
        }
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize()
            )
        }
        if (state.error.isNotEmpty()) {
            Text(
                text = "An error occured",
                modifier = Modifier
                    .fillMaxSize()
            )
            Log.e("MediaError", state.error)
        }
    }
}