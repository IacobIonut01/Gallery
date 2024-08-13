package com.dot.gallery.feature_node.presentation.edit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import com.dot.gallery.feature_node.presentation.util.newImageLoader
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditActivity : ComponentActivity() {

    @OptIn(ExperimentalCoilApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            GalleryTheme(
                darkTheme = true
            ) {
                setSingletonImageLoaderFactory(::newImageLoader)
                val viewModel = hiltViewModel<EditViewModel>()
                LaunchedEffect(intent.data) {
                    intent.data?.let {
                        viewModel.loadImage(it)
                    }
                }
                EditScreen(
                    viewModel = viewModel,
                    onNavigateUp = ::finish
                )
            }
        }
    }

    companion object {

        fun launchEditor(context: Context, uri: Uri) {
            context.startActivity(Intent(context, EditActivity::class.java).apply { data = uri })
        }
    }

}