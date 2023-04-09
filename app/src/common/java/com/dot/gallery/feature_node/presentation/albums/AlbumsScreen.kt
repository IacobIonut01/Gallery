package com.dot.gallery.feature_node.presentation.albums

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.rememberGlidePreloadingData
import com.bumptech.glide.signature.MediaStoreSignature
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.FilterButton
import com.dot.gallery.core.presentation.components.FilterOption
import com.dot.gallery.core.presentation.components.LoadingMedia
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.presentation.albums.components.AlbumComponent
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val state by rememberSaveable {
        viewModel.albumsState
    }
    val preloadingData = rememberGlidePreloadingData(
        data = state.albums,
        preloadImageSize = Size(50f, 50f)
    ) { item: Album, requestBuilder: RequestBuilder<Drawable> ->
        requestBuilder.load(item.pathToThumbnail)
            .signature(MediaStoreSignature(null, item.timestamp, 0))
    }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val filterRecent = stringResource(R.string.filter_recent)
    val filterOld = stringResource(R.string.filter_old)
    val filterNameAZ = stringResource(R.string.filter_nameAZ)
    val filterNameZA = stringResource(R.string.filter_nameZA)

    val filterOptions = remember {
        arrayOf(
            FilterOption(
                title = filterRecent,
                mediaOrder = MediaOrder.Date(OrderType.Descending),
                onClick = { viewModel.updateOrder(it) },
                selected = true
            ),
            FilterOption(
                title = filterOld,
                mediaOrder = MediaOrder.Date(OrderType.Ascending),
                onClick = { viewModel.updateOrder(it) }
            ),
            FilterOption(
                title = filterNameAZ,
                mediaOrder = MediaOrder.Label(OrderType.Ascending),
                onClick = { viewModel.updateOrder(it) }
            ),
            FilterOption(
                title = filterNameZA,
                mediaOrder = MediaOrder.Label(OrderType.Descending),
                onClick = { viewModel.updateOrder(it) }
            )
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.nav_albums),
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxSize(),
            columns = GridCells.Adaptive(Dimens.Album()),
            contentPadding = PaddingValues(
                top = it.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate(Screen.TrashedScreen.route)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(id = R.string.trash)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(id = R.string.trash)
                            )
                        }
                        Spacer(modifier = Modifier.size(16.dp))
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate(Screen.FavoriteScreen.route)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(id = R.string.favorites)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(id = R.string.favorites)
                            )
                        }
                    }
                }
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    FilterButton(filterOptions = filterOptions)
                }
                items(state.albums.size) { index ->
                    val (album, preloadRequestBuilder) = preloadingData[index]
                    AlbumComponent(album = album, preloadRequestBuilder) {
                        navController.navigate(Screen.AlbumViewScreen.route + "?albumId=${album.id}&albumName=${album.label}")
                    }
                }
            }
        )
        /** Error State Handling Block **/
        AnimatedVisibility(
            visible = state.isLoading,
            enter = Constants.Animation.enterAnimation,
            exit = Constants.Animation.exitAnimation,
        ) {
            LoadingMedia(modifier = Modifier.fillMaxSize())
        }
        if (!state.isLoading) {
            if (state.albums.isEmpty()) {
                EmptyMedia(modifier = Modifier.fillMaxSize())
            }
            if (state.error.isNotEmpty()) {
                Error(errorMessage = state.error)
            }
        }
        /** ************ **/
    }
}