package com.dot.gallery.feature_node.presentation.ignored.setup

sealed class IgnoredSetupDestination(val route: String) {
    data object Label : IgnoredSetupDestination("label")
    data object Location : IgnoredSetupDestination("location")
    data object Type : IgnoredSetupDestination("type")
    data object MatchedAlbums : IgnoredSetupDestination("matched_albums")

    operator fun invoke() = route
}