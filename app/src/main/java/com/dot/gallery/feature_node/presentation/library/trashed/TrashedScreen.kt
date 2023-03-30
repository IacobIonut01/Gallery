package com.dot.gallery.feature_node.presentation.library.trashed

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.media.MediaComponent
import com.dot.gallery.core.presentation.components.util.StickyHeaderGrid
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.domain.model.isHeaderKey
import com.dot.gallery.feature_node.presentation.library.trashed.components.EmptyTrash
import com.dot.gallery.feature_node.presentation.photos.components.StickyHeader
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.deleteMedia
import com.dot.gallery.feature_node.presentation.util.getDate
import com.dot.gallery.feature_node.presentation.util.trashMedia
import com.dot.gallery.ui.theme.Dimens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun TrashedGridScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    albumName: String = stringResource(id = R.string.trash),
    viewModel: TrashedViewModel = hiltViewModel(),
) {

    /** STRING BLOCK **/
    val stringToday = stringResource(id = R.string.header_today)
    val stringYesterday = stringResource(id = R.string.header_yesterday)

    /** ************ **/

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
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                selectionState.value = false
                selectedMedia.clear()
            }
        }
    )

    BackHandler(enabled = selectionState.value) {
        selectionState.value = false
        selectedMedia.clear()
    }
    /** ************  **/

    /**
     * This block requires recomputing on state change
     * avoid 'remember'
     */
    val mappedData = ArrayList<MediaItem>()
    state.media.groupBy {
        it.timestamp.getDate(
            stringToday = stringToday,
            stringYesterday = stringYesterday
        )
    }.forEach { (date, data) ->
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
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = albumName
                        )
                        if (selectionState.value) {
                            Text(
                                modifier = Modifier,
                                text = stringResource(R.string.selected_s, selectedMedia.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.media.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    context.trashMedia(
                                        result,
                                        selectedMedia.ifEmpty { state.media },
                                        false
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.trash_restore),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (selectionState.value) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        context.deleteMedia(result, selectedMedia)
                                    }
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.trash_delete),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        context.deleteMedia(result, state.media)
                                    }
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.trash_empty),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
                        is MediaItem.Header -> StickyHeader(date = item.text)
                        is MediaItem.MediaViewItem -> {
                            val (media, preloadRequestBuilder) = preloadingData[state.media.indexOf(
                                item.media
                            )]
                            MediaComponent(
                                media = media,
                                selectionState = selectionState,
                                isSelected = selectedMedia.find { it.id == media.id } != null,
                                preloadRequestBuilder = preloadRequestBuilder,
                                onItemLongClick = {
                                    viewModel.toggleSelection(state.media.indexOf(it))
                                },
                                onItemClick = {
                                    if (selectionState.value) {
                                        viewModel.toggleSelection(state.media.indexOf(it))
                                    } else {
                                        navController.navigate(Screen.MediaViewScreen.route + "?mediaId=${it.id}&target=trash")
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
    if (state.media.isEmpty()) {
        EmptyTrash(modifier = Modifier.fillMaxSize())
    }
    if (state.error.isNotEmpty()) {
        Error(errorMessage = state.error)
    }
    /** ************ **/
}