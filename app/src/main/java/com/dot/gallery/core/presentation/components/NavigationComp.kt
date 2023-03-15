package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dot.gallery.feature_node.presentation.photos.PhotosScreen
import com.dot.gallery.feature_node.presentation.util.Screen

@Composable
fun NavigationComp(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.PhotosScreen.route
    ) {
        composable(route = Screen.PhotosScreen.route) {
            PhotosScreen(
                navController = navController,
                topPadding = paddingValues.calculateTopPadding()
            )
        }
        composable(route = Screen.AlbumsScreen.route) {
            /*AlbumScreen(
                navController = navController, 
                topPadding = paddingValues.calculateTopPadding()
            )*/
        }
        composable(
            route = Screen.MediaScreen.route +
                    "?mediaId={mediaId}",
            arguments = listOf(
                navArgument(name = "mediaId") {
                    type = NavType.LongType
                    defaultValue = -1
                }
            )
        ) {
            /*MediaScreen(
                navController = navController,
                topPadding = paddingValues.calculateTopPadding(),
                mediaId = mediaId
            )*/
        }
    }
}