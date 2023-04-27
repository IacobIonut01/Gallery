package com.dot.gallery.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.dot.gallery.R

@Composable
fun NavigationButton(
    albumId: Long,
    navigateUp: () -> Unit,
    clearSelection: () -> Unit,
    selectionState: MutableState<Boolean>,
    alwaysGoBack: Boolean,
) {
    val onClick: () -> Unit =
        if (albumId != -1L && !selectionState.value || alwaysGoBack) navigateUp
        else clearSelection
    val icon = if (albumId != -1L && !selectionState.value || alwaysGoBack) Icons.Default.ArrowBack
    else Icons.Default.Close
    if (albumId != -1L || selectionState.value || alwaysGoBack) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.back_cd)
            )
        }
    }
}