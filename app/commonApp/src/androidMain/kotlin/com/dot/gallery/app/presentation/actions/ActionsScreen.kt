package com.dot.gallery.app.presentation.actions

import androidx.compose.runtime.Composable
import com.dot.gallery.feature.viewer.ActionsViewModel
import com.dot.gallery.injection.injectViewModel



@Composable
fun ActionsScreen() {
    val viewModel = injectViewModel<ActionsViewModel> {actionsViewModel}
}
