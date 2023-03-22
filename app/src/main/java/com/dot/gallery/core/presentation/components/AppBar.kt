package com.dot.gallery.core.presentation.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.feature_node.presentation.util.BottomNavItem
import com.dot.gallery.feature_node.presentation.util.Screen

@Composable
fun AppBar(
    context: Context,
    menuClick: () -> Unit,
    searchClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
            .padding(top = 16.dp)
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable {
                        menuClick.invoke()
                    },
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Menu"
            )

            Text(
                modifier = Modifier
                    .weight(1f),
                text = context.getString(R.string.searchbar_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )

            Icon(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable {
                        searchClick.invoke()
                    },
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search"
            )

        }

    }
}

@Composable
fun Toolbar(
    navController: NavController,
    modifier: Modifier = Modifier,
    text: String,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp, top = 12.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        var showBackButton = remember { false }

        navBackStackEntry?.destination?.route?.let {
            showBackButton = it.contains(Screen.AlbumViewScreen.route)
        }
        Column(modifier = Modifier.height(48.dp)) {
            AnimatedVisibility(
                visible = showBackButton,
                enter = enterAnimation,
                exit = exitAnimation
            ) {
                Image(
                    imageVector = Icons.Outlined.ArrowBack,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    contentDescription = "Go back",
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable {
                            navController.navigateUp()
                        }
                )
            }
        }
        Text(
            modifier = Modifier
                .padding(top = 56.dp),
            text = text,
            style = MaterialTheme.typography.displaySmall,
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                modifier = Modifier
                    .padding(top = 8.dp),
                text = subtitle.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun BottomAppBar(
    bottomNavItems: List<BottomNavItem>,
    backStackEntry: State<NavBackStackEntry?>,
    navController: NavController,
    bottomBarState: MutableState<Boolean>
) {
    AnimatedVisibility(
        visible = bottomBarState.value,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        content = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                bottomNavItems.forEach { item ->
                    val selected =
                        item.route == backStackEntry.value?.destination?.route

                    NavigationBarItem(
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        onClick = {
                            if (!selected) {
                                navController.navigate(item.route) {
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
                        },
                        label = {
                            Text(
                                modifier = Modifier
                                    .height(16.dp),
                                text = item.name,
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = "${item.name} Icon",
                            )
                        }
                    )
                }
            }
        }
    )
}