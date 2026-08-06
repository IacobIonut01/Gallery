package com.dot.gallery.feature_node.presentation.frameextract.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.frameextract.FrameIdentity
import com.dot.gallery.feature_node.presentation.frameextract.FramePreview

@Composable
fun FrameFilmstrip(
    frames: List<FramePreview>,
    current: FrameIdentity?,
    selected: Set<FrameIdentity>,
    preferredTimeUs: Long,
    onFrameClick: (FrameIdentity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentIndex = frames.indexOfFirst { it.identity == current }
    LaunchedEffect(current, currentIndex, frames.size) {
        if (currentIndex >= 0) {
            listState.scrollToItem((currentIndex - 2).coerceAtLeast(0))
        }
    }
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp),
        state = listState,
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(frames, key = { it.identity.encode() }) { frame ->
            val isCurrent = frame.identity == current
            val isSelected = frame.identity in selected
            val preferred = preferredTimeUs >= 0L &&
                kotlin.math.abs(frame.identity.presentationTimeUs - preferredTimeUs) < 50_000L
            val description = stringResource(
                R.string.frame_picker_frame_accessibility,
                frame.identity.frameIndex,
                frame.identity.presentationTimeUs / 1000L,
            )
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 70.dp)
                    .semantics { contentDescription = description }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFrameClick(frame.identity) }
                    .then(
                        if (isCurrent) {
                            Modifier.border(
                                3.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp),
                            )
                        } else Modifier
                    )
            ) {
                Image(
                    bitmap = frame.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(2.dp),
                    )
                }
                if (preferred) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = Color.Yellow,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(4.dp)
                            .size(18.dp),
                    )
                }
            }
        }
    }
}
