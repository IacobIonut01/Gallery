package com.dot.gallery.core.presentation.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.feature_node.presentation.common.MediaViewModel

@Composable
fun MediaViewModel.ObserveCustomMediaState(onChange: MediaViewModel.() -> Unit) {
    val state by mediaState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        onChange()
    }
}