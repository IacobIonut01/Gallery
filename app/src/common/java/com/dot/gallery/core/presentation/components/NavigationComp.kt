/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Target.TARGET_FAVORITES
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.Settings
import com.dot.gallery.feature_node.presentation.albums.AlbumsScreen
import com.dot.gallery.feature_node.presentation.library.LibraryScreen
import com.dot.gallery.feature_node.presentation.library.favorites.FavoriteScreen
import com.dot.gallery.feature_node.presentation.library.trashed.TrashedGridScreen
import com.dot.gallery.feature_node.presentation.mediaview.MediaViewScreen
import com.dot.gallery.feature_node.presentation.timeline.TimelineScreen
import com.dot.gallery.feature_node.presentation.MediaViewModel
import com.dot.gallery.feature_node.presentation.albums.AlbumsViewModel
import com.dot.gallery.feature_node.presentation.settings.SettingsScreen
import com.dot.gallery.feature_node.presentation.settings.SettingsViewModel
import com.dot.gallery.feature_node.presentation.util.BottomNavItem
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
    bottomNavEntries: List<BottomNavItem>,
    settings: Settings
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    navBackStackEntry?.destination?.route?.let {
        bottomBarState.value = bottomNavEntries.firstOrNull { item -> item.route == it } != null
        systemBarFollowThemeState.value = !it.contains(Screen.MediaViewScreen.route)
    }
    AnimatedNavHost(
        navController = navController,
        startDestination = Screen.TimelineScreen.route
    ) {
        composable(
            route = Screen.TimelineScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation }
        ) {
            val viewModel = hiltViewModel<MediaViewModel>().apply {
                launchInPhotosScreen()
                initWithNav(navController)
            }
            TimelineScreen(
                paddingValues = paddingValues,
                retrieveMedia = viewModel::launchInPhotosScreen,
                handler = viewModel.handler,
                mediaState = viewModel.photoState,
                selectionState = viewModel.multiSelectState,
                selectedMedia = viewModel.selectedPhotoState,
                toggleSelection = viewModel::toggleSelection,
                navigate = viewModel::navigate,
                navigateUp = viewModel::navigateUp
            )
        }
        composable(
            route = Screen.TrashedScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation }
        ) {
            val viewModel = hiltViewModel<MediaViewModel>().apply {
                target = TARGET_TRASH
                initWithNav(navController)
            }
            TrashedGridScreen(
                paddingValues = paddingValues,
                mediaState = viewModel.photoState,
                selectionState = viewModel.multiSelectState,
                selectedMedia = viewModel.selectedPhotoState,
                handler = viewModel.handler,
                toggleSelection = viewModel::toggleSelection,
                navigate = viewModel::navigate,
                navigateUp = viewModel::navigateUp
            )
        }
        composable(
            route = Screen.FavoriteScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation }
        ) {
            val viewModel = hiltViewModel<MediaViewModel>().apply {
                target = TARGET_FAVORITES
                initWithNav(navController)
            }
            FavoriteScreen(
                paddingValues = paddingValues,
                mediaState = viewModel.photoState,
                selectionState = viewModel.multiSelectState,
                selectedMedia = viewModel.selectedPhotoState,
                toggleFavorite = viewModel::toggleFavorite,
                toggleSelection = viewModel::toggleSelection,
                navigate = viewModel::navigate,
                navigateUp = viewModel::navigateUp
            )
        }
        composable(
            route = Screen.LibraryScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation }
        ) {
            LibraryScreen(
                navController = navController
            )
        }
        composable(
            route = Screen.AlbumsScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation }
        ) {
            val viewModel = hiltViewModel<AlbumsViewModel>().apply {
                initWithNav(navController)
            }
            AlbumsScreen(
                navigate = viewModel::navigate,
                paddingValues = paddingValues,
                viewModel = viewModel,
                settings = settings
            )
        }
        composable(
            route = Screen.AlbumViewScreen.route +
                    "?albumId={albumId}&albumName={albumName}",
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation },
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
            val argumentAlbumName = backStackEntry.arguments?.getString("albumName") ?: stringResource(id = R.string.app_name)
            val argumentAlbumId = backStackEntry.arguments?.getLong("albumId") ?: -1
            val viewModel: MediaViewModel = hiltViewModel<MediaViewModel>().apply {
                albumId = argumentAlbumId
                initWithNav(navController)
            }
            TimelineScreen(
                paddingValues = paddingValues,
                albumId = argumentAlbumId,
                albumName = argumentAlbumName,
                handler = viewModel.handler,
                mediaState = viewModel.photoState,
                selectionState = viewModel.multiSelectState,
                selectedMedia = viewModel.selectedPhotoState,
                toggleSelection = viewModel::toggleSelection,
                navigate = viewModel::navigate,
                navigateUp = viewModel::navigateUp
            )
        }
        composable(
            route = Screen.MediaViewScreen.route +
                    "?mediaId={mediaId}&albumId={albumId}",
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation },
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
            val viewModel = hiltViewModel<MediaViewModel>(parentEntry).apply {
                initWithNav(navController)
            }
            MediaViewScreen(
                paddingValues = paddingValues,
                mediaId = mediaId,
                settings = settings,
                mediaState = viewModel.photoState,
                handler = viewModel.handler,
                navigateUp = viewModel::navigateUp
            )
        }
        composable(
            route = Screen.MediaViewScreen.route +
                    "?mediaId={mediaId}&target={target}",
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation },
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
            val viewModel = hiltViewModel<MediaViewModel>(parentEntry).apply {
                initWithNav(navController)
            }
            MediaViewScreen(
                paddingValues = paddingValues,
                mediaId = mediaId,
                target = target,
                settings = settings,
                mediaState = viewModel.photoState,
                handler = viewModel.handler,
                navigateUp = viewModel::navigateUp
            )
        }
        composable(
            route = Screen.SettingsScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation },
        ) {
            val viewModel = hiltViewModel<SettingsViewModel>().apply {
                initWithNav(navController)
            }
            SettingsScreen(navigateUp = viewModel::navigateUp)
        }
    }
}