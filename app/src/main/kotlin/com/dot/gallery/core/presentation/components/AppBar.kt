/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.util.NavigationItem
import com.dot.gallery.feature_node.presentation.util.Screen

@Composable
fun rememberNavigationItems(): List<NavigationItem> {
    val timelineTitle = stringResource(R.string.nav_timeline)
    val albumsTitle = stringResource(R.string.nav_albums)
    return remember {
        listOf(
            NavigationItem(
                name = timelineTitle,
                route = Screen.TimelineScreen.route,
                icon = Icons.Outlined.Photo,
            ),
            NavigationItem(
                name = albumsTitle,
                route = Screen.AlbumsScreen.route,
                icon = Icons.Outlined.PhotoAlbum,
            )
        )
    }
}

@Composable
fun AppBarContainer(
    windowSizeClass: WindowSizeClass,
    navController: NavController,
    bottomBarState: MutableState<Boolean>,
    content: @Composable () -> Unit,
) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val bottomNavItems = rememberNavigationItems()
    val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
    val showNavigation by bottomBarState
    val label: @Composable (item: NavigationItem) -> Unit = {
        Text(
            text = it.name,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    val icon: @Composable (item: NavigationItem) -> Unit = {
        Icon(
            imageVector = it.icon,
            contentDescription = "${it.name} Icon",
        )
    }
    val onClick: (route: String) -> Unit = remember {
        { navigate(navController, it) }
    }

    Box {
        Row {
            if (useNavRail && showNavigation) {
                AppNavigationRail(
                    backStackEntry = backStackEntry,
                    navigationItems = bottomNavItems,
                    onClick = onClick,
                    label = label,
                    icon = icon
                )
            }
            content.invoke()
        }

        if (!useNavRail) {
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = showNavigation,
                enter = slideInVertically { it * 2 },
                exit = slideOutVertically { it * 2 },
                content = {
                    AppNavigationBar(
                        backStackEntry = backStackEntry,
                        navigationItems = bottomNavItems,
                        onClick = onClick,
                        label = label,
                        icon = icon
                    )
                }
            )
        }
    }
}

private fun navigate(navController: NavController, route: String) {
    navController.navigate(route) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination when
        // reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

@Composable
private fun AppNavigationBar(
    backStackEntry: State<NavBackStackEntry?>,
    navigationItems: List<NavigationItem>,
    onClick: (route: String) -> Unit,
    label: @Composable (item: NavigationItem) -> Unit,
    icon: @Composable (item: NavigationItem) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    ) {
        navigationItems.forEach { item ->
            val selected = item.route == backStackEntry.value?.destination?.route
            NavigationBarItem(
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                onClick = {
                    if (!selected) {
                        onClick(item.route)
                    }
                },
                label = { label(item) },
                icon = { icon(item) }
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    backStackEntry: State<NavBackStackEntry?>,
    navigationItems: List<NavigationItem>,
    onClick: (route: String) -> Unit,
    label: @Composable (item: NavigationItem) -> Unit,
    icon: @Composable (item: NavigationItem) -> Unit,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(Modifier.weight(1f))
        navigationItems.forEach { item ->
            val selected = item.route == backStackEntry.value?.destination?.route
            NavigationRailItem(
                selected = selected,
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                onClick = {
                    if (!selected) {
                        onClick(item.route)
                    }
                },
                label = { label(item) },
                icon = { icon(item) }
            )
        }
        Spacer(Modifier.weight(1f))
    }

}