/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dot.gallery.R
import com.dot.gallery.core.Settings.Misc.rememberOldNavbar
import com.dot.gallery.feature_node.presentation.util.NavigationItem
import com.dot.gallery.feature_node.presentation.util.Screen

@Composable
fun rememberNavigationItems(): List<NavigationItem> {
    val timelineTitle = stringResource(R.string.nav_timeline)
    val albumsTitle = stringResource(R.string.nav_albums)
    val searchTitle = stringResource(R.string.nav_search)
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
            ),
            NavigationItem(
                name = searchTitle,
                route = Screen.SearchScreen.route,
                icon = Icons.Outlined.Search,
            )
        )
    }
}

@Composable
fun AppBarContainer(
    windowSizeClass: WindowSizeClass,
    navController: NavController,
    bottomBarState: MutableState<Boolean>,
    paddingValues: PaddingValues,
    isScrolling: MutableState<Boolean>,
    content: @Composable () -> Unit,
) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val bottomNavItems = rememberNavigationItems()
    val useNavRail = remember {
        windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
    }
    val useOldNavbar by rememberOldNavbar()

    if (useOldNavbar) {
        Box {
            val showNavRail by remember(useNavRail, bottomBarState.value) {
                mutableStateOf(useNavRail && bottomBarState.value)
            }
            AnimatedVisibility(
                visible = showNavRail,
                enter = slideInHorizontally { it * -2 },
                exit = slideOutHorizontally { it * -2 }
            ) {
                ClassicNavigationRail(
                    backStackEntry = backStackEntry,
                    navigationItems = bottomNavItems,
                    onClick = { navigate(navController, it) }
                )
            }
            val animatedPadding by animateDpAsState(
                targetValue = remember(useNavRail, bottomBarState.value) {
                    if (useNavRail && bottomBarState.value) 80.dp else 0.dp
                },
                label = "animatedPadding"
            )
            Box(
                modifier = Modifier.padding(start = animatedPadding)
            ) {
                content()
            }
            val showClassicNavbar by remember(useNavRail, isScrolling.value, bottomBarState.value) {
                mutableStateOf(!useNavRail && bottomBarState.value && !isScrolling.value)
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = showClassicNavbar,
                enter = slideInVertically { it * 2 },
                exit = slideOutVertically { it * 2 },
                content = {
                    ClassicNavBar(
                        backStackEntry = backStackEntry,
                        navigationItems = bottomNavItems,
                        onClick = { navigate(navController, it) },
                    )
                }
            )
        }
    } else {
        Box {
            content()
            val showNavbar by remember(bottomBarState.value, isScrolling.value) {
                mutableStateOf(bottomBarState.value && !isScrolling.value)
            }
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                visible = showNavbar,
                enter = slideInVertically { it * 2 },
                exit = slideOutVertically { it * 2 },
                content = {
                    val modifier = remember(useNavRail) {
                        if (useNavRail) Modifier.requiredWidth((110 * bottomNavItems.size).dp)
                        else Modifier.fillMaxWidth()
                    }
                    GalleryNavBar(
                        modifier = modifier,
                        backStackEntry = backStackEntry,
                        navigationItems = bottomNavItems,
                        onClick = { navigate(navController, it) }
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
fun GalleryNavBar(
    modifier: Modifier,
    backStackEntry: State<NavBackStackEntry?>,
    navigationItems: List<NavigationItem>,
    onClick: (route: String) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 32.dp, vertical = 32.dp)
            .then(modifier)
            .height(64.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                shape = RoundedCornerShape(percent = 100)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationItems.forEach { item ->
            val selected = item.route == backStackEntry.value?.destination?.route
            GalleryNavBarItem(
                navItem = item,
                isSelected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
fun ClassicNavBar(
    backStackEntry: State<NavBackStackEntry?>,
    navigationItems: List<NavigationItem>,
    onClick: (route: String) -> Unit
) {
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
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun ClassicNavigationRail(
    backStackEntry: State<NavBackStackEntry?>,
    navigationItems: List<NavigationItem>,
    onClick: (route: String) -> Unit
) {
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

@Composable
fun RowScope.GalleryNavBarItem(
    navItem: NavigationItem,
    isSelected: Boolean,
    onClick: (route: String) -> Unit,
) {
    val mutableInteraction = remember { MutableInteractionSource() }
    val selectedColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "selectedColor"
    )
    val selectedIconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "selectedIconColor"
    )
    Box(
        modifier = Modifier
            .height(64.dp)
            .weight(1f)
            // Dummy clickable to intercept clicks from passing under the container
            .clickable(
                indication = null,
                interactionSource = mutableInteraction,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(64.dp)
                .background(
                    color = selectedColor,
                    shape = RoundedCornerShape(percent = 100)
                )
                .clip(RoundedCornerShape(100))
                .clickable { if (!isSelected) onClick(navItem.route) },
        )
        Icon(
            modifier = Modifier
                .size(22.dp),
            imageVector = navItem.icon,
            contentDescription = navItem.name,
            tint = selectedIconColor
        )
    }
}