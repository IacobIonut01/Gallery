/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.standalone

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import com.dot.gallery.core.AlbumState
import com.dot.gallery.feature_node.presentation.mediaview.MediaViewScreen
import com.dot.gallery.feature_node.presentation.util.newImageLoader
import com.dot.gallery.feature_node.presentation.util.toggleOrientation
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class StandaloneActivity : ComponentActivity() {

    @OptIn(ExperimentalCoilApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val action = intent.action.toString()
        val isSecure = action.lowercase().contains("secure")
        val clipData = intent.clipData
        val uriList = mutableSetOf<Uri>()
        intent.data?.let(uriList::add)
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                uriList.add(clipData.getItemAt(i).uri)
            }
        }
        setShowWhenLocked(isSecure)
        enableEdgeToEdge()
        setContent {
            setSingletonImageLoaderFactory(::newImageLoader)
            GalleryTheme(darkTheme = true) {
                Scaffold { paddingValues ->
                    val viewModel = hiltViewModel<StandaloneViewModel>().apply {
                        reviewMode = action.lowercase().contains("review")
                        dataList = uriList.toList()
                    }

                    MediaViewScreen(
                        navigateUp = { finish() },
                        toggleRotate = ::toggleOrientation,
                        paddingValues = paddingValues,
                        isStandalone = true,
                        mediaId = viewModel.mediaId,
                        mediaState = viewModel.mediaState,
                        albumsState = MutableStateFlow(AlbumState()),
                        handler = viewModel.handler
                    )
                }
                BackHandler {
                    finish()
                }
            }
        }
    }
}