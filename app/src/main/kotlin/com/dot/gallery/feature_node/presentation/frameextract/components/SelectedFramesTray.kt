package com.dot.gallery.feature_node.presentation.frameextract.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.frameextract.FrameIdentity
import com.dot.gallery.feature_node.presentation.frameextract.FramePreview

@Composable
fun SelectedFramesTray(
    frames: List<FrameIdentity>,
    thumbnails: List<FramePreview>,
    onJump: (FrameIdentity) -> Unit,
    onRemove: (FrameIdentity) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (frames.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.frame_picker_selected_count, frames.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            TextButton(onClick = onClear) { Text(stringResource(R.string.frame_picker_clear)) }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(frames, key = FrameIdentity::encode) { frame ->
                val thumbnail = thumbnails.firstOrNull { it.identity == frame }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onJump(frame) },
                ) {
                    thumbnail?.let {
                        Image(
                            bitmap = it.bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    Text(
                        text = if (frame.frameIndex >= 0) "#${frame.frameIndex}" else "${frame.presentationTimeUs / 1000L} ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                    IconButton(
                        onClick = { onRemove(frame) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), CircleShape),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.frame_picker_remove_accessibility),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }
    }
}
