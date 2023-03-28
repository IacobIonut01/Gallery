package com.dot.gallery.core.presentation.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.presentation.albums.AlbumsScreen
import com.dot.gallery.feature_node.presentation.library.LibraryScreen
import com.dot.gallery.feature_node.presentation.library.favorites.FavoriteScreen
import com.dot.gallery.feature_node.presentation.library.trashed.TrashedGridScreen
import com.dot.gallery.feature_node.presentation.mediaview.MediaViewScreen
import com.dot.gallery.feature_node.presentation.photos.PhotosScreen
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
    bottomNavEntries: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    navBackStackEntry?.destination?.route?.let {
        bottomBarState.value = bottomNavEntries.firstOrNull { item -> item.route == it } != null
        systemBarFollowThemeState.value = !it.contains(Screen.MediaViewScreen.route)
    }
    AnimatedNavHost(
        navController = navController,
        startDestination = Screen.PhotosScreen.route
    ) {
        composable(
            route = Screen.PhotosScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation }
        ) {
            PhotosScreen(
                navController = navController,
                paddingValues = paddingValues
            )
        }
        composable(
            route = Screen.TrashedScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation }
        ) {
            TrashedGridScreen(
                navController = navController,
                paddingValues = paddingValues
            )
        }
        composable(
            route = Screen.FavoriteScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation }
        ) {
            FavoriteScreen(
                navController = navController,
                paddingValues = paddingValues
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
                navController = navController,
                paddingValues = paddingValues
            )
        }
        composable(
            route = Screen.AlbumsScreen.route,
            enterTransition = { Constants.Animation.navigateInAnimation },
            exitTransition = { Constants.Animation.navigateUpAnimation },
            popEnterTransition = { Constants.Animation.navigateInAnimation },
            popExitTransition = { Constants.Animation.navigateUpAnimation }
        ) {
            AlbumsScreen(
                navController = navController,
                paddingValues = paddingValues
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
        ) {
            it.arguments?.let { args ->
                val albumId = args.getLong("albumId")
                val albumName = args.getString("albumName")
                if (albumId != -1L && !albumName.isNullOrEmpty()) {
                    PhotosScreen(
                        navController = navController,
                        paddingValues = paddingValues,
                        albumId = albumId,
                        albumName = albumName,
                    )
                }
            }
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
        ) {
            it.arguments?.let { args ->
                val mediaId = args.getLong("mediaId")
                val albumId = args.getLong("albumId")
                MediaViewScreen(
                    navController = navController,
                    paddingValues = paddingValues,
                    mediaId = mediaId,
                    albumId = albumId
                )
            }
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
        ) {
            it.arguments?.let { args ->
                val mediaId = args.getLong("mediaId")
                val target = args.getString("target")
                MediaViewScreen(
                    navController = navController,
                    paddingValues = paddingValues,
                    mediaId = mediaId,
                    target = target,
                )
            }
        }
    }
}