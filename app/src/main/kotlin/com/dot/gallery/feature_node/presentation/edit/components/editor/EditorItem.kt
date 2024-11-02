package com.dot.gallery.feature_node.presentation.edit.components.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EditorItem(
    imageVector: ImageVector,
    title: String,
    enabled: Boolean = true,
    horizontal: Boolean = false,
    onItemLongClick: (() -> Unit)? = null,
    onItemClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    val tintColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    val modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .defaultMinSize(
            minWidth = 90.dp,
            minHeight = 80.dp
        )
        .combinedClickable(
            enabled = enabled,
            onLongClick = onItemLongClick,
            onClick = onItemClick
        )
        .padding(vertical = 16.dp)
    if (horizontal) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(12.dp)
                )
                .then(modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                imageVector = imageVector,
                colorFilter = ColorFilter.tint(tintColor),
                contentDescription = title,
                modifier = Modifier
                    .padding(16.dp)
                    .size(28.dp)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge,
                color = tintColor,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(24.dp))
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageVector = imageVector,
                colorFilter = ColorFilter.tint(tintColor),
                contentDescription = title,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .height(32.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = title,
                modifier = Modifier,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = tintColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}