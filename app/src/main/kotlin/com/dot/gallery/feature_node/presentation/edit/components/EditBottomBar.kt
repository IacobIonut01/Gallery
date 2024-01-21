package com.dot.gallery.feature_node.presentation.edit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.ui.theme.GalleryTheme

@Composable
fun EditBottomBar(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCancel: () -> Unit,
    onOverride: () -> Unit,
    onSaveCopy: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onCancel, enabled = enabled) {
            Icon(
                imageVector = Icons.Outlined.Close,
                tint = Color.White,
                contentDescription = stringResource(id = R.string.action_cancel)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            /*OutlinedButton(onClick = onOverride, enabled = enabled) {
                Text(text = stringResource(R.string.override))
            }*/
            Button(onClick = onSaveCopy, enabled = enabled) {
                Text(text = stringResource(R.string.save_copy))
            }
        }
    }
}

@Preview(showBackground = true, wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE)
@Composable
private fun Preview() {
    GalleryTheme(
        darkTheme = true
    ) {
        Surface(
            color = Color.Black
        ) {
            EditBottomBar(
                onCancel = { /*TODO*/ },
                onOverride = { /*TODO*/ },
                onSaveCopy = {}
            )
        }
    }
}