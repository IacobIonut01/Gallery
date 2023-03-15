package com.dot.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dot.gallery.core.presentation.components.AppBar
import com.dot.gallery.core.presentation.components.BottomAppBar
import com.dot.gallery.core.presentation.components.NavigationComp
import com.dot.gallery.feature_node.presentation.util.BottomNavItem
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bottomNavItems = listOf(
            BottomNavItem(
                name = getString(R.string.nav_photos),
                route = Screen.PhotosScreen.route,
                icon = Icons.Outlined.Photo,
            ),
            BottomNavItem(
                name = getString(R.string.nav_albums),
                route = Screen.AlbumsScreen.route,
                icon = Icons.Outlined.PhotoAlbum,
            ),
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            GalleryTheme {
                val navController = rememberNavController()
                val backStackEntry = navController.currentBackStackEntryAsState()
                Scaffold(
                    bottomBar = {
                        BottomAppBar(
                            bottomNavItems = bottomNavItems,
                            backStackEntry = backStackEntry,
                            navController = navController
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                    content = { paddingValues ->
                        NavigationComp(
                            navController = navController,
                            paddingValues = paddingValues
                        )
                        AppBar(context = this@MainActivity,
                            menuClick = { /*TODO*/ },
                            searchClick = { /*TODO*/ }
                        )
                    }
                )
            }
        }
    }
}