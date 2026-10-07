/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.dot.gallery.core.presentation.util.Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.core.ui.components.settings
import com.dot.gallery.app.presentation.albums.AlbumsScreen
import com.dot.gallery.app.presentation.library.LibraryScreen
import com.dot.gallery.core.navigate


@MediumTest
@RunWith(AndroidJUnit4::class)
class StartupNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun initialAlbumsRouteIsTheFirstDestination() {
        rule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = rememberStartupDestination(Screen.AlbumsScreen())
            ) {
                composable(Screen.AlbumsScreen()) { Text("albums") }
                composable(Screen.LibraryScreen()) { Text("library") }
            }
        }
        rule.onNodeWithText("albums").assertIsDisplayed()
    }

    @Test
    fun initialLibraryRouteIsTheFirstDestination() {
        rule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = rememberStartupDestination(Screen.LibraryScreen())
            ) {
                composable(Screen.AlbumsScreen()) { Text("albums") }
                composable(Screen.LibraryScreen()) { Text("library") }
            }
        }
        rule.onNodeWithText("library").assertIsDisplayed()
    }

    @Test
    fun changingTheSuppliedInitialDoesNotRebuildTheGraph() {
        var supplied by mutableStateOf(Screen.AlbumsScreen())
        var navController: NavHostController? = null
        rule.setContent {
            val controller = rememberNavController().also { navController = it }
            NavHost(
                navController = controller,
                startDestination = rememberStartupDestination(supplied)
            ) {
                composable(Screen.AlbumsScreen()) { Text("albums") }
                composable(Screen.LibraryScreen()) { Text("library") }
                composable("settings_test") { Text("settings") }
            }
        }
        rule.onNodeWithText("albums").assertIsDisplayed()

        rule.runOnUiThread { navController!!.navigate("settings_test") }
        rule.onNodeWithText("settings").assertIsDisplayed()

        rule.runOnUiThread { supplied = Screen.LibraryScreen() }
        rule.waitForIdle()
        rule.onNodeWithText("settings").assertIsDisplayed()
        rule.onNodeWithText("library").assertDoesNotExist()
    }
}
