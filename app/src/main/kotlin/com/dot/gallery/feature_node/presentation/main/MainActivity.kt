/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.main

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.dot.gallery.core.Settings.Misc.getSecureMode
import com.dot.gallery.core.Settings.Misc.rememberForceTheme
import com.dot.gallery.core.Settings.Misc.rememberIsDarkMode
import com.dot.gallery.core.presentation.components.AppBarContainer
import com.dot.gallery.core.presentation.components.NavigationComp
import com.dot.gallery.feature_node.presentation.util.toggleOrientation
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enforceSecureFlag()
        enableEdgeToEdge()
        setContent {
            GalleryTheme {
                val navController = rememberNavController()
                val isScrolling = remember { mutableStateOf(false) }
                val bottomBarState = rememberSaveable { mutableStateOf(true) }
                val systemBarFollowThemeState = rememberSaveable { mutableStateOf(true) }
                val forcedTheme by rememberForceTheme()
                val localDarkTheme by rememberIsDarkMode()
                val systemDarkTheme = isSystemInDarkTheme()
                val darkTheme by remember(forcedTheme, localDarkTheme, systemDarkTheme) {
                    mutableStateOf(if (forcedTheme) localDarkTheme else systemDarkTheme)
                }
                LaunchedEffect(darkTheme, systemBarFollowThemeState.value) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            Color.TRANSPARENT,
                            Color.TRANSPARENT,
                        ) { darkTheme || !systemBarFollowThemeState.value },
                        navigationBarStyle = SystemBarStyle.auto(
                            Color.TRANSPARENT,
                            Color.TRANSPARENT,
                        ) { darkTheme || !systemBarFollowThemeState.value }
                    )
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { paddingValues ->
                        AppBarContainer(
                            navController = navController,
                            paddingValues = paddingValues,
                            bottomBarState = bottomBarState.value,
                            isScrolling = isScrolling.value
                        ) {
                            NavigationComp(
                                navController = navController,
                                paddingValues = paddingValues,
                                bottomBarState = bottomBarState,
                                systemBarFollowThemeState = systemBarFollowThemeState,
                                toggleRotate = ::toggleOrientation,
                                isScrolling = isScrolling
                            )
                        }
                    }
                )
            }
        }
    }

    private fun enforceSecureFlag() {
        lifecycleScope.launch {
            getSecureMode(this@MainActivity).collectLatest { enabled ->
                if (enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }

}