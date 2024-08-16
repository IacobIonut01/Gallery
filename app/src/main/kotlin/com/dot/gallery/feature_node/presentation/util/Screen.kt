/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.util

sealed class Screen(val route: String) {
    data object TimelineScreen : Screen("timeline_screen")
    data object AlbumsScreen : Screen("albums_screen")

    data object AlbumViewScreen : Screen("album_view_screen") {

        fun albumAndName() = "$route?albumId={albumId}&albumName={albumName}"

    }
    data object MediaViewScreen : Screen("media_screen") {

        fun idAndTarget() = "$route?mediaId={mediaId}&target={target}"

        fun idAndAlbum() = "$route?mediaId={mediaId}&albumId={albumId}"
    }

    data object TrashedScreen : Screen("trashed_screen")
    data object FavoriteScreen : Screen("favorite_screen")

    data object SettingsScreen : Screen("settings_screen")
    data object IgnoredScreen : Screen("ignored_screen")
    data object IgnoredSetupScreen : Screen("ignored_setup_screen")

    data object SetupScreen: Screen("setup_screen")

    data object VaultScreen : Screen("vault_screen")
    data object EncryptedMediaViewScreen : Screen("encrypted_media_view_screen") {
        fun id() = "$route?mediaId={mediaId}"

        fun id(id: Long) = "$route?mediaId=$id"
    }

    data object LibraryScreen : Screen("library_screen")

    operator fun invoke() = route
}
