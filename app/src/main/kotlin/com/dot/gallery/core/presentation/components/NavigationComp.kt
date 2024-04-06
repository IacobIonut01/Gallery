/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Animation.navigateInAnimation
import com.dot.gallery.core.Constants.Animation.navigateUpAnimation
import com.dot.gallery.core.Constants.Target.TARGET_FAVORITES
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.Settings.Album.rememberHideTimelineOnAlbum
import com.dot.gallery.core.Settings.Misc.rememberLastScreen
import com.dot.gallery.core.Settings.Misc.rememberTimelineGroupByMonth
import com.dot.gallery.core.presentation.components.util.ObserveCustomMediaState
import com.dot.gallery.core.presentation.components.util.OnLifecycleEvent
import com.dot.gallery.core.presentation.components.util.permissionGranted
import com.dot.gallery.feature_node.presentation.albums.AlbumsScreen
import com.dot.gallery.feature_node.presentation.albums.AlbumsViewModel
import com.dot.gallery.feature_node.presentation.common.ChanneledViewModel
import com.dot.gallery.feature_node.presentation.common.MediaViewModel
import com.dot.gallery.feature_node.presentation.favorites.FavoriteScreen
import com.dot.gallery.feature_node.presentation.ignored.IgnoredScreen
import com.dot.gallery.feature_node.presentation.mediaview.MediaViewScreen
import com.dot.gallery.feature_node.presentation.settings.SettingsScreen
import com.dot.gallery.feature_node.presentation.setup.SetupScreen
import com.dot.gallery.feature_node.presentation.timeline.TimelineScreen
import com.dot.gallery.feature_node.presentation.trashed.TrashedGridScreen
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.newImageLoader

@OptIn(ExperimentalCoilApi::class)
@Composable
fun NavigationComp(
    navController: NavHostController,
    paddingValues: PaddingValues,
    bottomBarState: MutableState<Boolean>,
    systemBarFollowThemeState: MutableState<Boolean>,
    toggleRotate: () -> Unit,
    isScrolling: MutableState<Boolean>
) {
    val searchBarActive = rememberSaveable {
        mutableStateOf(false)
    }
    val bottomNavEntries = rememberNavigationItems()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val navPipe = hiltViewModel<ChanneledViewModel>()
    navPipe
        .initWithNav(navController, bottomBarState)
        .collectAsStateWithLifecycle(LocalLifecycleOwner.current)
    val groupTimelineByMonth by rememberTimelineGroupByMonth()

    val context = LocalContext.current
    var permissionState by remember { mutableStateOf(context.permissionGranted(Constants.PERMISSIONS)) }
    var lastStartScreen by rememberLastScreen()
    val startDest = remember(permissionState, lastStartScreen) {
        if (permissionState) {
            lastStartScreen
        } else Screen.SetupScreen()
    }
    val currentDest = remember(navController.currentDestination) {
        navController.currentDestination?.route ?: lastStartScreen
    }
    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_PAUSE) {
            if (currentDest == Screen.TimelineScreen() || currentDest == Screen.AlbumsScreen()) {
                lastStartScreen = currentDest
            }
        }
    }

    var lastShouldDisplay by rememberSaveable {
        mutableStateOf(bottomNavEntries.find { item -> item.route == currentDest } != null)
    }
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.destination?.route?.let {
            val shouldDisplayBottomBar =
                bottomNavEntries.find { item -> item.route == it } != null
            if (lastShouldDisplay != shouldDisplayBottomBar) {
                bottomBarState.value = shouldDisplayBottomBar
                lastShouldDisplay = shouldDisplayBottomBar
            }
            systemBarFollowThemeState.value = !it.contains(Screen.MediaViewScreen.route)
        }
    }

    // Preloaded viewModels
    val albumsViewModel = hiltViewModel<AlbumsViewModel>().apply {
        attachToLifecycle()
    }

    val timelineViewModel = hiltViewModel<MediaViewModel>().apply {
        attachToLifecycle()
    }
    val searchViewModel = hiltViewModel<MediaViewModel>().apply {
        attachToLifecycle()
    }
    LaunchedEffect(groupTimelineByMonth) {
        timelineViewModel.groupByMonth = groupTimelineByMonth
    }

    setSingletonImageLoaderFactory(::newImageLoader)

    NavHost(
        navController = navController,
        startDestination = startDest,
        enterTransition = { navigateInAnimation },
        exitTransition = { navigateUpAnimation },
        popEnterTransition = { navigateInAnimation },
        popExitTransition = { navigateUpAnimation }
    ) {
        composable(
            route = Screen.SetupScreen(),
        ) {
            navPipe.toggleNavbar(false)
            SetupScreen {
                permissionState = true
                navPipe.navigate(Screen.TimelineScreen())
            }
        }
        composable(
            route = Screen.TimelineScreen()
        ) {
            TimelineScreen(
                vm = timelineViewModel,
                paddingValues = paddingValues,
                handler = timelineViewModel.handler,
                mediaState = timelineViewModel.mediaState,
                albumState = albumsViewModel.albumsState,
                selectionState = timelineViewModel.multiSelectState,
                selectedMedia = timelineViewModel.selectedPhotoState,
                toggleSelection = timelineViewModel::toggleSelection,
                navigate = navPipe::navigate,
                navigateUp = navPipe::navigateUp,
                toggleNavbar = navPipe::toggleNavbar,
                isScrolling = isScrolling,
                searchBarActive = searchBarActive,
            )
        }
        composable(
            route = Screen.TrashedScreen()
        ) {
            val viewModel = hiltViewModel<MediaViewModel>()
                .apply { target = TARGET_TRASH }
                .apply { groupByMonth = groupTimelineByMonth }
            viewModel.attachToLifecycle()
            TrashedGridScreen(
                vm = viewModel,
                paddingValues = paddingValues,
                mediaState = viewModel.mediaState,
                albumState = albumsViewModel.albumsState,
                selectionState = viewModel.multiSelectState,
                selectedMedia = viewModel.selectedPhotoState,
                handler = viewModel.handler,
                toggleSelection = viewModel::toggleSelection,
                navigate = navPipe::navigate,
                navigateUp = navPipe::navigateUp,
                toggleNavbar = navPipe::toggleNavbar
            )
        }
        composable(
            route = Screen.FavoriteScreen()
        ) {
            timelineViewModel.ObserveCustomMediaState(MediaViewModel::getFavoriteMedia)
            FavoriteScreen(
                vm = timelineViewModel,
                paddingValues = paddingValues,
                mediaState = timelineViewModel.customMediaState,
                albumState = albumsViewModel.albumsState,
                handler = timelineViewModel.handler,
                selectionState = timelineViewModel.multiSelectState,
                selectedMedia = timelineViewModel.selectedPhotoState,
                toggleFavorite = timelineViewModel::toggleFavorite,
                toggleSelection = timelineViewModel::toggleCustomSelection,
                navigate = navPipe::navigate,
                navigateUp = navPipe::navigateUp,
                toggleNavbar = navPipe::toggleNavbar
            )
        }
        composable(
            route = Screen.AlbumsScreen()
        ) {
            AlbumsScreen(
                mediaViewModel = timelineViewModel,
                navigate = navPipe::navigate,
                toggleNavbar = navPipe::toggleNavbar,
                paddingValues = paddingValues,
                viewModel = albumsViewModel,
                isScrolling = isScrolling,
                searchBarActive = searchBarActive
            )
        }
        composable(
            route = Screen.AlbumViewScreen.albumAndName(),
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
            val appName = stringResource(id = R.string.app_name)
            val argumentAlbumName = remember(backStackEntry) {
                backStackEntry.arguments?.getString("albumName") ?: appName
            }
            val argumentAlbumId = remember(backStackEntry) {
                backStackEntry.arguments?.getLong("albumId") ?: -1
            }
            timelineViewModel.ObserveCustomMediaState {
                getMediaFromAlbum(argumentAlbumId)
            }
            val hideTimeline by rememberHideTimelineOnAlbum()
            TimelineScreen(
                vm = timelineViewModel,
                paddingValues = paddingValues,
                albumId = argumentAlbumId,
                albumName = argumentAlbumName,
                handler = timelineViewModel.handler,
                mediaState = timelineViewModel.customMediaState,
                albumState = albumsViewModel.albumsState,
                selectionState = timelineViewModel.multiSelectState,
                selectedMedia = timelineViewModel.selectedPhotoState,
                allowNavBar = false,
                allowHeaders = !hideTimeline,
                enableStickyHeaders = !hideTimeline,
                toggleSelection = timelineViewModel::toggleCustomSelection,
                navigate = navPipe::navigate,
                navigateUp = navPipe::navigateUp,
                toggleNavbar = navPipe::toggleNavbar,
                isScrolling = isScrolling
            )
        }
        composable(
            route = Screen.MediaViewScreen.idAndAlbum(),
            arguments = listOf(
                navArgument(name = "mediaId") {
                    type = NavType.LongType
                    defaultValue = -1
                },
                navArgument(name = "albumId") {
                    type = NavType.LongType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val mediaId: Long = remember(backStackEntry) {
                backStackEntry.arguments?.getLong("mediaId") ?: -1L
            }
            val albumId: Long = remember(backStackEntry) {
                backStackEntry.arguments?.getLong("albumId") ?: -1L
            }
            if (albumId != -1L) {
                timelineViewModel.ObserveCustomMediaState {
                    getMediaFromAlbum(albumId)
                }
            }
            MediaViewScreen(
                navigateUp = navPipe::navigateUp,
                toggleRotate = toggleRotate,
                paddingValues = paddingValues,
                mediaId = mediaId,
                mediaState = if (albumId != -1L) timelineViewModel.customMediaState else timelineViewModel.mediaState,
                albumsState = albumsViewModel.albumsState,
                handler = timelineViewModel.handler
            )
        }
        composable(
            route = Screen.MediaViewScreen.idAndTarget(),
            arguments = listOf(
                navArgument(name = "mediaId") {
                    type = NavType.LongType
                    defaultValue = -1
                },
                navArgument(name = "target") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val mediaId: Long = remember(backStackEntry) {
                backStackEntry.arguments?.getLong("mediaId") ?: -1
            }
            val target: String? = remember(backStackEntry) {
                backStackEntry.arguments?.getString("target")
            }
            val entryName = remember(target) {
                when (target) {
                    TARGET_FAVORITES -> Screen.FavoriteScreen.route
                    TARGET_TRASH -> Screen.TrashedScreen.route
                    else -> Screen.TimelineScreen.route
                }
            }
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(entryName)
            }
            val viewModel = if (target == TARGET_FAVORITES) {
                timelineViewModel.also {
                    timelineViewModel.ObserveCustomMediaState(MediaViewModel::getFavoriteMedia)
                }
            } else {
                hiltViewModel<MediaViewModel>(parentEntry).also {
                    it.attachToLifecycle()
                }
            }
            MediaViewScreen(
                navigateUp = navPipe::navigateUp,
                toggleRotate = toggleRotate,
                paddingValues = paddingValues,
                mediaId = mediaId,
                target = target,
                mediaState = if (target == TARGET_FAVORITES) viewModel.customMediaState else viewModel.mediaState,
                albumsState = albumsViewModel.albumsState,
                handler = viewModel.handler
            )
        }
        composable(
            route = Screen.SettingsScreen()
        ) {
            SettingsScreen(
                navigateUp = navPipe::navigateUp,
                navigate = navPipe::navigate
            )
        }
        composable(
            route = Screen.BlacklistScreen()
        ) {
            val albumsState by albumsViewModel.albumsState.collectAsStateWithLifecycle()
            IgnoredScreen(
                navigateUp = navPipe::navigateUp,
                albumsState = albumsState
            )
        }
        composable(
            route = Screen.SearchScreen()
        ) {
            com.dot.gallery.feature_node.presentation.search.SearchScreen(
                navigate = navPipe::navigate,
                paddingValues = paddingValues,
                isScrolling = isScrolling,
                mediaViewModel = searchViewModel,
                toggleNavbar = navPipe::toggleNavbar,
                searchBarActive = searchBarActive
            )
        }
    }
}