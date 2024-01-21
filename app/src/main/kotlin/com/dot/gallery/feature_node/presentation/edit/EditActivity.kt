package com.dot.gallery.feature_node.presentation.edit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            GalleryTheme(
                darkTheme = true
            ) {
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

}