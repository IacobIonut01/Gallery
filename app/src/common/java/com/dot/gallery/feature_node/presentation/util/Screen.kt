package com.dot.gallery.feature_node.presentation.util

sealed class Screen(val route: String) {
    object PhotosScreen : Screen("photos_screen")
    object AlbumsScreen : Screen("albums_screen")

    object AlbumViewScreen : Screen("album_view_screen")
    object MediaViewScreen : Screen("media_screen")

    object LibraryScreen : Screen("library_screen")

    object TrashedScreen : Screen("trashed_screen")
    object FavoriteScreen : Screen("favorite_screen")
}
