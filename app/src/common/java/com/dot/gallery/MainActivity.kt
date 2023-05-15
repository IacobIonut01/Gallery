/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dot.gallery.core.Settings.Misc.rememberIsMediaManager
import com.dot.gallery.core.presentation.components.BottomAppBar
import com.dot.gallery.core.presentation.components.NavigationComp
import com.dot.gallery.feature_node.presentation.util.BottomNavItem
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.theme.GalleryTheme
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import android.provider.Settings as AndroidSettings

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bottomNavItems = listOf(
            BottomNavItem(
                name = getString(R.string.nav_timeline),
                route = Screen.TimelineScreen.route,
                icon = Icons.Outlined.Photo,
            ),
            BottomNavItem(
                name = getString(R.string.nav_albums),
                route = Screen.AlbumsScreen.route,
                icon = Icons.Outlined.PhotoAlbum,
            ),
            /*
            BottomNavItem(
                name = getString(R.string.nav_library),
                route = Screen.LibraryScreen.route,
                icon = Icons.Outlined.PhotoLibrary,
            ),*/
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
            GalleryTheme {
                val navController = rememberAnimatedNavController()
                val backStackEntry = navController.currentBackStackEntryAsState()
                val bottomBarState = rememberSaveable { (mutableStateOf(true)) }
                val systemBarFollowThemeState = rememberSaveable { (mutableStateOf(true)) }
                val systemUiController = rememberSystemUiController()
                systemUiController.systemBarsDarkContentEnabled =
                    systemBarFollowThemeState.value && !isSystemInDarkTheme()
                Scaffold(
                    bottomBar = {
                        if (!useNavRail) {
                            BottomAppBar(
                                bottomNavItems = bottomNavItems,
                                backStackEntry = backStackEntry,
                                navController = navController,
                                bottomBarState = bottomBarState
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    content = { paddingValues ->
                        Row {
                            if (useNavRail && bottomBarState.value) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    Spacer(Modifier.weight(1f))
                                    bottomNavItems.forEach { item ->
                                        val selected =
                                            item.route == backStackEntry.value?.destination?.route
                                        NavigationRailItem(
                                            selected = selected,
                                            colors = NavigationRailItemDefaults.colors(
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
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                            NavigationComp(
                                navController = navController,
                                paddingValues = paddingValues,
                                bottomBarState = bottomBarState,
                                systemBarFollowThemeState = systemBarFollowThemeState,
                                bottomNavEntries = bottomNavItems
                            )
                        }
                    }
                )
                RequestPermission()
            }
        }
    }

    @Composable
    private fun RequestPermission() {
        var useMediaManager by rememberIsMediaManager()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            if (!MediaStore.canManageMedia(this) &&
                /** Don't ask every launch,
                 * user might want tot use an extra confirmation dialog for dangerous actions
                 **/
                !useMediaManager
            ) {
                val intent = Intent()
                intent.action = AndroidSettings.ACTION_REQUEST_MANAGE_MEDIA
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.data = uri
                startActivity(intent)
                useMediaManager = true
            }
        }
    }
}