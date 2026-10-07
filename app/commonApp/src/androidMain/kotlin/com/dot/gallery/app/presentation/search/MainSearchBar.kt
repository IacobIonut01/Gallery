package com.dot.gallery.app.presentation.search

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.navigate
import com.dot.gallery.core.presentation.util.Screen
import com.dot.gallery.core.presentation.components.MainSearchBar as SharedMainSearchBar

/**
 * App-side shim over the `:core:ui` [SharedMainSearchBar] — keeps the legacy signature while
 * wiring the search-route navigation through the app's `Screen` table.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainSearchBar(
    isScrolling: MutableState<Boolean>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    menuItems: @Composable (RowScope.() -> Unit)? = null,
    searchBarTrailingIcon: @Composable (() -> Unit)? = null,
    statusBarInsets: WindowInsets = WindowInsets.statusBars,
) {
    val eventHandler = LocalEventHandler.current
    SharedMainSearchBar(
        isScrolling = isScrolling,
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
        onSearchClick = { eventHandler.navigate(Screen.SearchScreen()) },
        menuItems = menuItems,
        searchBarTrailingIcon = searchBarTrailingIcon,
        statusBarInsets = statusBarInsets,
    )
}
