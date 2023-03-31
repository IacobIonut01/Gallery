package com.dot.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.dot.gallery.feature_node.presentation.standalone.StandaloneMediaViewScreen
import com.dot.gallery.feature_node.presentation.standalone.StandaloneViewModel
import com.dot.gallery.feature_node.presentation.util.FileUtils
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StandaloneActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uriPath: String? = intent.data?.let { FileUtils(this).getPath(it) }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            GalleryTheme(darkTheme = true) {
                Scaffold { paddingValues ->
                    val viewModel = hiltViewModel<StandaloneViewModel>()
                    StandaloneMediaViewScreen(paddingValues, uriPath, viewModel)
                }
                BackHandler {
                    finish()
                }
            }
        }
    }
}