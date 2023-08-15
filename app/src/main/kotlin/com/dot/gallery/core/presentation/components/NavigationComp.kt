/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.navigateInAnimation
import com.dot.gallery.core.Constants.Animation.navigateUpAnimation
import com.dot.gallery.core.Constants.Target.TARGET_FAVORITES
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.Settings.Misc.rememberTimelineGroupByMonth
import com.dot.gallery.feature_node.presentation.albums.AlbumsScreen
import com.dot.gallery.feature_node.presentation.albums.AlbumsViewModel
import com.dot.gallery.feature_node.presentation.common.ChanneledViewModel
import com.dot.gallery.feature_node.presentation.common.MediaViewModel
import com.dot.gallery.feature_node.presentation.favorites.FavoriteScreen
import com.dot.gallery.feature_node.presentation.mediaview.MediaViewScreen
import com.dot.gallery.feature_node.presentation.settings.SettingsScreen
import com.dot.gallery.feature_node.presentation.settings.SettingsViewModel
import com.dot.gallery.feature_node.presentation.settings.customization.albumsize.AlbumSizeScreen
import com.dot.gallery.feature_node.presentation.timeline.TimelineScreen
import com.dot.gallery.feature_node.presentation.trashed.TrashedGridScreen
import com.dot.gallery.feature_node.presentation.util.Screen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationComp(
    navController: NavHostController,
    paddingValues: PaddingValues,
    bottomBarState: MutableState<Boolean>,
    systemBarFollowThemeState: MutableState<Boolean>,
    windowSizeClass: WindowSizeClass,
    toggleRotate: () -> Unit,
    isScrolling: MutableState<Boolean>
) {
    val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
    val bottomNavEntries = rememberNavigationItems()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    navBackStackEntry?.destination?.route?.let {
        val shouldDisplayBottomBar = bottomNavEntries.find { item -> item.route == it } != null
        bottomBarState.value = shouldDisplayBottomBar
        systemBarFollowThemeState.value = !it.contains(Screen.MediaViewScreen.route)
    }
    val navPipe = hiltViewModel<ChanneledViewModel>()
    navPipe
        .initWithNav(navController, bottomBarState)
        .collectAsStateWithLifecycle(LocalLifecycleOwner.current)

    val groupTimelineByMonth by rememberTimelineGroupByMonth()

    AnimatedNavHost(
        navController = navController,
        startDestination = Screen.TimelineScreen.route
    ) {
        composable(
            route = Screen.TimelineScreen.route,
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation }
        ) {
            val viewModel = hiltViewModel<MediaViewModel>().apply {
                groupByMonth = groupTimelineByMonth
            }
            viewModel.attachToLifecycle()
            TimelineScreen(
                paddingValues = paddingValues,
                retrieveMedia = viewModel::launchInPhotosScreen,
                handler = viewModel.handler,
                mediaState = viewModel.mediaState,
                selectionState = viewModel.multiSelectState,
                selectedMedia = viewModel.selectedPhotoState,
                toggleSelection = viewModel::toggleSelection,
                allowNavBar = !useNavRail,
                navigate = navPipe::navigate,
                navigateUp = navPipe::navigateUp,
                toggleNavbar = navPipe::toggleNavbar,
                isScrolling = isScrolling,
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
            val viewModel = hiltViewModel<MediaViewModel>()
                .apply { target = TARGET_FAVORITES }
                .apply { groupByMonth = groupTimelineByMonth }
            viewModel.attachToLifecycle()
            FavoriteScreen(
                paddingValues = paddingValues,
                mediaState = viewModel.mediaState,
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
            val viewModel = hiltViewModel<AlbumsViewModel>()
            viewModel.attachToLifecycle()
            AlbumsScreen(
                navigate = navPipe::navigate,
                toggleNavbar = navPipe::toggleNavbar,
                paddingValues = paddingValues,
                viewModel = viewModel,
                isScrolling = isScrolling,
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
            val argumentAlbumName = backStackEntry.arguments?.getString("albumName")
                ?: stringResource(id = R.string.app_name)
            val argumentAlbumId = backStackEntry.arguments?.getLong("albumId") ?: -1
            val viewModel: MediaViewModel = hiltViewModel<MediaViewModel>()
                .apply { albumId = argumentAlbumId }
                .apply { groupByMonth = groupTimelineByMonth }
            viewModel.attachToLifecycle()
            TimelineScreen(
                paddingValues = paddingValues,
                albumId = argumentAlbumId,
                albumName = argumentAlbumName,
                handler = viewModel.handler,
                mediaState = viewModel.mediaState,
                selectionState = viewModel.multiSelectState,
                selectedMedia = viewModel.selectedPhotoState,
                allowNavBar = false,
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
            val mediaId: Long = backStackEntry.arguments?.getLong("mediaId") ?: -1
            val albumId: Long = backStackEntry.arguments?.getLong("albumId") ?: -1
            val entryName =
                if (albumId == -1L) Screen.TimelineScreen.route else Screen.AlbumViewScreen.route
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(entryName)
            }
            val viewModel = hiltViewModel<MediaViewModel>(parentEntry)
            viewModel.attachToLifecycle()
            MediaViewScreen(
                paddingValues = paddingValues,
                mediaId = mediaId,
                mediaState = viewModel.mediaState,
                handler = viewModel.handler,
                navigateUp = navPipe::navigateUp,
                toggleRotate = toggleRotate
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
            val mediaId: Long = backStackEntry.arguments?.getLong("mediaId") ?: -1
            val target: String? = backStackEntry.arguments?.getString("target")
            val entryName = when (target) {
                TARGET_FAVORITES -> Screen.FavoriteScreen.route
                TARGET_TRASH -> Screen.TrashedScreen.route
                else -> Screen.TimelineScreen.route
            }
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(entryName)
            }
            val viewModel = hiltViewModel<MediaViewModel>(parentEntry)
            viewModel.attachToLifecycle()
            MediaViewScreen(
                paddingValues = paddingValues,
                mediaId = mediaId,
                target = target,
                mediaState = viewModel.mediaState,
                handler = viewModel.handler,
                navigateUp = navPipe::navigateUp,
                toggleRotate = toggleRotate
            )
        }
        composable(
            route = Screen.SettingsScreen.route,
            enterTransition = { navigateInAnimation },
            exitTransition = { navigateUpAnimation },
            popEnterTransition = { navigateInAnimation },
            popExitTransition = { navigateUpAnimation },
        ) {
            val viewModel = hiltViewModel<SettingsViewModel>()
            SettingsScreen(
                navigateUp = navPipe::navigateUp,
                navigate = navPipe::navigate,
                viewModel = viewModel
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
    }
}