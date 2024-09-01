package com.dot.gallery.feature_node.presentation.vault

sealed class VaultScreens(val route: String) {
    data object VaultSetup : VaultScreens("vault_setup")
    data object VaultDisplay : VaultScreens("vault_display")

    data object EncryptedMediaViewScreen : VaultScreens("vault_media_view_screen") {
        fun id() = "$route?mediaId={mediaId}"

        fun id(id: Long) = "$route?mediaId=$id"
    }

    data object LoadingScreen : VaultScreens("vault_loading_screen")

    operator fun invoke() = route
}