package com.dot.gallery

import android.app.PendingIntent.getActivity
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dot.gallery.core.presentation.components.BottomAppBar
import com.dot.gallery.core.presentation.components.NavigationComp
import com.dot.gallery.feature_node.presentation.util.BottomNavItem
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.theme.GalleryTheme
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalAnimationApi::class)
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

        val deleteResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
            registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) {
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(Environment.getExternalStorageDirectory().toString()),
                    null
                ) { _: String?, _: Uri? -> }
            }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
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
                        BottomAppBar(
                            bottomNavItems = bottomNavItems,
                            backStackEntry = backStackEntry,
                            navController = navController,
                            bottomBarState = bottomBarState
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                    content = { paddingValues ->
                        NavigationComp(
                            navController = navController,
                            paddingValues = paddingValues,
                            bottomBarState = bottomBarState,
                            deleteResultLauncher = deleteResultLauncher,
                            systemBarFollowThemeState = systemBarFollowThemeState
                        )
                        /*
                        AppBar(context = this@MainActivity,
                            menuClick = { /*TODO*/ },
                            searchClick = { /*TODO*/ }
                        )
                         */
                    }
                )
            }
        }
    }
}