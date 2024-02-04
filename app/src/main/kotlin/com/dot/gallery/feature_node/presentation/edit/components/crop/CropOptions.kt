package com.dot.gallery.feature_node.presentation.edit.components.crop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.ImageAspectRatio
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dot.gallery.ui.theme.GalleryTheme

@Composable
fun CropOptions(
    modifier: Modifier = Modifier,
    onMirrorPressed: () -> Unit,
    onRotatePressed: () -> Unit,
    onAspectRationPressed: () -> Unit,
    onCropPressed: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMirrorPressed) {
            Icon(
                imageVector = Icons.Outlined.Flip,
                contentDescription = "Mirror",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onAspectRationPressed) {
            Icon(
                imageVector = Icons.Outlined.ImageAspectRatio,
                contentDescription = "Aspect Ratio",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRotatePressed) {
            Icon(
                imageVector = Icons.Outlined.Rotate90DegreesCw,
                contentDescription = "Rotate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onCropPressed) {
            Text(text = "Apply")
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GalleryTheme(
        darkTheme = true
    ) {
        Surface(
            color = Color.Black
        ) {
            CropOptions(
                onMirrorPressed = { },
                onRotatePressed = { },
                onAspectRationPressed = { },
                onCropPressed = {}
            )
        }
    }
}