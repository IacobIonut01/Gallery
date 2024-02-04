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
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Animation.navigateInAnimation
import com.dot.gallery.core.Constants.Animation.navigateUpAnimation
import com.dot.gallery.core.Constants.Target.TARGET_FAVORITES
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.Settings.Album.rememberHideTimelineOnAlbum
import com.dot.gallery.core.Settings.Misc.rememberLastScreen
import com.dot.gallery.core.Settings.Misc.rememberTimelineGroupByMonth
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
import com.dot.gallery.feature_node.presentation.settings.customization.albumsize.AlbumSizeScreen
import com.dot.gallery.feature_node.presentation.setup.SetupScreen
import com.dot.gallery.feature_node.presentation.timeline.TimelineScreen
import com.dot.gallery.feature_node.presentation.trashed.TrashedGridScreen
import com.dot.gallery.feature_node.presentation.util.Screen

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
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.destination?.route?.let {
            val shouldDisplayBottomBar =
                bottomNavEntries.find { item -> item.route == it && !searchBarActive.value } != null
            bottomBarState.value = shouldDisplayBottomBar
            systemBarFollowThemeState.value = !it.contains(Screen.MediaViewScreen.route)
        }
    }
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

    // Preloaded viewModels
    val albumsViewModel = hiltViewModel<AlbumsViewModel>().apply {
        attachToLifecycle()
    }

    val timelineViewModel = hiltViewModel<MediaViewModel>().apply {
        attachToLifecycle()
    }
    LaunchedEffect(groupTimelineByMonth) {
        timelineViewModel.groupByMonth = groupTimelineByMonth
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(
            route = Screen.SetupScreen(),
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation }
        ) {
            navPipe.toggleNavbar(false)
            SetupScreen {
                permissionState = true
                navPipe.navigate(Screen.TimelineScreen())
            }
        }
        composable(
            route = Screen.TimelineScreen.route,
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation }
        ) {
            TimelineScreen(
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
            route = Screen.TrashedScreen.route,
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation }
        ) {
            val viewModel = hiltViewModel<MediaViewModel>()
                .apply { target = TARGET_TRASH }
                .apply { groupByMonth = groupTimelineByMonth }
            viewModel.attachToLifecycle()
            TrashedGridScreen(
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
            route = Screen.FavoriteScreen.route,
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation }
        ) {
            val viewModel = timelineViewModel.also {
                it.getFavoriteMedia()
            }
            viewModel.attachToLifecycle()
            FavoriteScreen(
                paddingValues = paddingValues,
                mediaState = viewModel.customMediaState,
                albumState = albumsViewModel.albumsState,
                handler = viewModel.handler,
                selectionState = viewModel.multiSelectState,
                selectedMedia = viewModel.selectedPhotoState,
                toggleFavorite = viewModel::toggleFavorite,
                toggleSelection = viewModel::toggleSelection,
                navigate = navPipe::navigate,
                navigateUp = navPipe::navigateUp,
                toggleNavbar = navPipe::toggleNavbar
            )
        }
        composable(
            route = Screen.AlbumsScreen.route,
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation }
        ) {
            AlbumsScreen(
                navigate = navPipe::navigate,
                toggleNavbar = navPipe::toggleNavbar,
                paddingValues = paddingValues,
                viewModel = albumsViewModel,
                isScrolling = isScrolling,
                searchBarActive = searchBarActive
            )
        }
        composable(
            route = Screen.AlbumViewScreen.route +
                    "?albumId={albumId}&albumName={albumName}",
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
            val appName = stringResource(id = R.string.app_name)
            val argumentAlbumName = remember(backStackEntry) {
                backStackEntry.arguments?.getString("albumName") ?: appName
            }
            val argumentAlbumId = remember(backStackEntry) {
                backStackEntry.arguments?.getLong("albumId") ?: -1
            }
            val viewModel: MediaViewModel = timelineViewModel.also {
                it.getMediaFromAlbum(argumentAlbumId)
            }
            val hideTimeline by rememberHideTimelineOnAlbum()
            TimelineScreen(
                paddingValues = paddingValues,
                albumId = argumentAlbumId,
                albumName = argumentAlbumName,
                handler = viewModel.handler,
                mediaState = viewModel.customMediaState,
                albumState = albumsViewModel.albumsState,
                selectionState = viewModel.multiSelectState,
                selectedMedia = viewModel.selectedPhotoState,
                allowNavBar = false,
                allowHeaders = !hideTimeline,
                enableStickyHeaders = !hideTimeline,
                toggleSelection = viewModel::toggleSelection,
                navigate = navPipe::navigate,
                navigateUp = navPipe::navigateUp,
                toggleNavbar = navPipe::toggleNavbar,
                isScrolling = isScrolling
            )
        }
        composable(
            route = Screen.MediaViewScreen.route +
                    "?mediaId={mediaId}&albumId={albumId}",
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation },
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
            route = Screen.MediaViewScreen.route +
                    "?mediaId={mediaId}&target={target}",
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation },
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
                    it.getFavoriteMedia()
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
            route = Screen.SettingsScreen.route,
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation },
        ) {
            SettingsScreen(
                navigateUp = navPipe::navigateUp,
                navigate = navPipe::navigate
            )
        }
        composable(
            route = Screen.AlbumSizeScreen.route,
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation },
        ) {
            AlbumSizeScreen(
                navigateUp = navPipe::navigateUp
            )
        }
        composable(
            route = Screen.BlacklistScreen(),
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation },
        ) {
            val albumsState by albumsViewModel.albumsState.collectAsStateWithLifecycle()
            IgnoredScreen(
                navigateUp = navPipe::navigateUp,
                albumsState = albumsState
            )
        }
    }
}