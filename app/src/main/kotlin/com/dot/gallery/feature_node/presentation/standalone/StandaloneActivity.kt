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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.feature_node.domain.model.AlbumState
import com.dot.gallery.feature_node.presentation.mediaview.MediaViewScreen
import com.dot.gallery.feature_node.presentation.util.setHdrMode
import com.dot.gallery.feature_node.presentation.util.toggleOrientation
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StandaloneActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
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
        setContent {
            GalleryTheme {
                Scaffold { paddingValues ->
                    val viewModel = hiltViewModel<StandaloneViewModel>()
                    LaunchedEffect(Unit) {
                        viewModel.reviewMode = action.lowercase().contains("review")
                        viewModel.dataList = uriList.toList()
                    }
                    val vaults = viewModel.vaults.collectAsStateWithLifecycle()
                    val mediaState = viewModel.mediaState.collectAsStateWithLifecycle()
                    MediaViewScreen(
                        navigateUp = { finish() },
                        toggleRotate = ::toggleOrientation,
                        paddingValues = paddingValues,
                        isStandalone = true,
                        mediaId = viewModel.mediaId,
                        mediaState = mediaState,
                        albumsState = remember {
                            mutableStateOf(AlbumState())
                        },
                        handler = viewModel.handler,
                        addMedia = viewModel::addMedia,
                        vaultState = vaults
                    )
                }
                BackHandler {
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setHdrMode(true)
    }

    override fun onPause() {
        super.onPause()
        setHdrMode(false)
    }
}