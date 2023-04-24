/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.dot.gallery.core.Settings
import com.dot.gallery.feature_node.presentation.standalone.StandaloneMediaViewScreen
import com.dot.gallery.feature_node.presentation.standalone.StandaloneViewModel
import com.dot.gallery.feature_node.presentation.util.uriToPath
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StandaloneActivity : ComponentActivity() {

    @Inject
    lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val action = intent.action.toString()
        val isSecure = action.toLowerCase(Locale.current).contains("secure")
        val clipData = intent.clipData
        var clipDataUris: List<Uri> = emptyList()
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                clipDataUris = emptyList<Uri>().toMutableList().apply { add(i, clipData.getItemAt(i).uri) }
            }
        }
        setShowWhenLocked(isSecure)
        setContent {
            GalleryTheme(darkTheme = true) {
                Scaffold { paddingValues ->
                    val viewModel = hiltViewModel<StandaloneViewModel>().apply {
                        standaloneUri = uriToPath(intent.data)
                        clipDataUriList = clipDataUris
                    }
                    StandaloneMediaViewScreen(paddingValues, settings, viewModel)
                }
                BackHandler {
                    finish()
                }
            }
        }
    }
}