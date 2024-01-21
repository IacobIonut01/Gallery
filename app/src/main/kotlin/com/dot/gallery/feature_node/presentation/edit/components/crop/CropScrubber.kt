package com.dot.gallery.feature_node.presentation.edit.components.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.dot.gallery.ui.theme.GalleryTheme
import kotlin.math.roundToInt

@Composable
fun CropScrubber(
    modifier: Modifier = Modifier,
    onAngleChanged: (newAngle: Float) -> Unit
) {
    var currentAngle by rememberSaveable { mutableFloatStateOf(0f) }
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "${currentAngle.roundToInt()}°",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier
                .padding(start = 6.dp)
                .padding(bottom = 8.dp)
                .align(Alignment.CenterHorizontally)
        )
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxWidth()
        ) {
            val config = LocalConfiguration.current
            val displayWidth = remember(config) {
                config.screenWidthDp.dp
            }
            val scrubberPadding = remember(displayWidth) { displayWidth / 2 }

            val spacerWidth = remember { 4.dp }
            val normalWidth = remember { 2.dp }
            val normalHeight = remember { 10.dp }
            val arrowHeight = remember { 18.dp }

            val normalColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.4f
            )
            val highlightedColor = MaterialTheme.colorScheme.onSurface
            val arrowColor = MaterialTheme.colorScheme.primary

            val maxAngle = remember { 45 }
            val middleIndex = remember { 48 }

            /**
             * Range 0 (left) - 48 (middle) - 96 (right)
             */
            val state = rememberLazyListState(
                initialFirstVisibleItemIndex = middleIndex
            )

            LaunchedEffect(state) {
                snapshotFlow { state.firstVisibleItemIndex }.collect {
                    fun value(raw: Int): Float = raw / middleIndex.toFloat() * maxAngle
                    currentAngle = if (it < middleIndex) - maxAngle + value(it)
                    else if (it > middleIndex) value(it - middleIndex)
                    else 0f
                    onAngleChanged(currentAngle)
                }
            }
            LazyRow(
                state = state,
                modifier = Modifier.wrapContentWidth(),
                contentPadding = PaddingValues(horizontal = scrubberPadding),
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(2) {
                    item {
                        Spacer(
                            modifier = Modifier
                                .height(normalHeight)
                                .width(normalWidth)
                                .background(normalColor)
                        )
                    }
                    item {
                        Spacer(
                            modifier = Modifier
                                .height(normalHeight)
                                .width(spacerWidth)
                        )
                    }
                }
                repeat(4) {
                    item {
                        Spacer(
                            modifier = Modifier
                                .height(if (it == 2) arrowHeight else normalHeight)
                                .width(normalWidth)
                                .background(highlightedColor)
                        )
                    }
                    item {
                        Spacer(
                            modifier = Modifier
                                .height(normalHeight)
                                .width(spacerWidth)
                        )
                    }
                    repeat(10) {
                        item {
                            Spacer(
                                modifier = Modifier
                                    .height(normalHeight)
                                    .width(normalWidth)
                                    .background(normalColor)
                            )
                        }
                        item {
                            Spacer(
                                modifier = Modifier
                                    .height(normalHeight)
                                    .width(spacerWidth)
                            )
                        }
                    }
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .height(normalHeight)
                            .width(normalWidth)
                            .background(highlightedColor)
                    )
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .height(normalHeight)
                            .width(spacerWidth)
                    )
                }
                repeat(2) { index ->
                    item {
                        Spacer(
                            modifier = Modifier
                                .height(normalHeight)
                                .width(normalWidth)
                                .background(normalColor)
                        )
                    }
                    if (index == 0) {
                        item {
                            Spacer(
                                modifier = Modifier
                                    .height(normalHeight)
                                    .width(spacerWidth)
                            )
                        }
                    }
                }
            }

            /**
             * Selection Arrow
             */
            Spacer(
                modifier = Modifier
                    .height(arrowHeight)
                    .width(normalWidth)
                    .background(arrowColor)
            )
        }
    }
}

@Preview(wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE, showBackground = true)
@Composable
private fun Preview() {
    GalleryTheme(
        darkTheme = true
    ) {
        var rotation by remember { mutableFloatStateOf(0f) }
        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(256.dp)
                        .rotate(rotation)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(24.dp)
                        )
                )
                Spacer(modifier = Modifier.height(64.dp))
                CropScrubber(onAngleChanged = {
                    rotation = it
                })
            }
        }
    }
}