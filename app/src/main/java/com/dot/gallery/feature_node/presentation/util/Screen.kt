package com.dot.gallery.feature_node.presentation.util

sealed class Screen(val route: String) {
    object PhotosScreen: Screen("photos_screen")
    object AlbumsScreen: Screen("albums_screen")
    object MediaScreen: Screen("media_screen")
}
