package com.dot.gallery.feature_node.presentation.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.presentation.util.FeedbackManager.Companion.rememberFeedbackManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun BoxScope.TimelineScroller(
    gridState: LazyGridState,
    mappedData: List<MediaItem>,
    paddingValues: PaddingValues
) {
    val scope = rememberCoroutineScope()
    val offsets = remember { mutableStateMapOf<Int, Float>() }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val feedbackManager = rememberFeedbackManager()
    fun scrollIfNeeded(offset: Float) {
        val index = offsets
            .mapValues { abs(it.value - offset) }
            .entries
            .minByOrNull { it.value }
            ?.key ?: return
        if (selectedIndex == index) return
        selectedIndex = index
        scope.launch {
            feedbackManager.vibrate()
            gridState.scrollToItem(selectedIndex, scrollOffset = -250)
        }
    }

    var scrollVisible by remember { mutableStateOf(false) }
    val scrollAlpha by animateFloatAsState(
        animationSpec = tween(durationMillis = 1000),
        targetValue = if (scrollVisible) 1f else 0f,
        label = "scrollAlpha"
    )

    val configuration = LocalConfiguration.current
    val height = remember(configuration) { configuration.screenHeightDp }
    val headerList =
        remember(mappedData) { mappedData.filterIsInstance<MediaItem.Header>() }
    val bottomPadding = remember { 32 /*dp*/ }

    /**
     * Distribute scroll items height proportionally to the amount of date headers
     **/
    val parentTopPadding = remember(paddingValues) { paddingValues.calculateTopPadding().value }
    val parentBottomPadding = remember { paddingValues.calculateBottomPadding().value }
    val heightSize =
        remember(height, parentTopPadding, parentBottomPadding, bottomPadding, headerList) {
            (height - parentTopPadding - parentBottomPadding - bottomPadding) / headerList.size
        }

    // W.I.P.
    /*val yearList = remember { mutableStateListOf<String>() }
    LazyColumn(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(paddingValues)
            .padding(top = 32.dp, bottom = bottomPadding.dp)
            .padding(end = 48.dp)
            .fillMaxHeight()
            .wrapContentWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.End
    ) {
        itemsIndexed(
            items = headerList,
            key = { _, item -> item.key }
        ) { index, item ->
            val year = getYear(item.text)
            if (!yearList.contains(year)) {
                yearList.add(year)
                val padding = heightSize * index
                Log.d(
                    Constants.TAG,
                    "Height size: $heightSize, Index: $index, Top padding for $year: $padding"
                )
                Text(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = -padding
                        }
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(100)
                        )
                        .padding(vertical = 4.dp, horizontal = 5.dp),
                    text = year,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }*/

    Box(
        modifier = Modifier
            .padding(paddingValues)
            .padding(top = 32.dp, bottom = bottomPadding.dp)
            .graphicsLayer {
                translationY = offsets[selectedIndex] ?: 0f
                alpha = scrollAlpha
            }
            .size(48.dp)
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(
                    topStartPercent = 100,
                    bottomStartPercent = 100
                )
            )
            .align(Alignment.TopEnd)
            .padding(vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_scroll_arrow),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiary
        )
    }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(top = 32.dp, bottom = bottomPadding.dp)
            .align(Alignment.TopEnd)
            .fillMaxHeight()
            .verticalScroll(state = rememberScrollState())
            .graphicsLayer {
                alpha = scrollAlpha
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        scope.launch {
                            scrollVisible = true
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            delay(1500)
                            scrollVisible = false
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            delay(500)
                            scrollVisible = false
                        }
                    },
                    onVerticalDrag = { change, _ ->
                        if (!scrollVisible) {
                            scrollVisible = true
                        }
                        scrollIfNeeded(change.position.y)
                    }
                )
            },
        verticalArrangement = Arrangement.Top
    ) {
        mappedData.forEachIndexed { i, header ->
            if (header is MediaItem.Header) {
                Spacer(
                    modifier = Modifier
                        .width(16.dp)
                        .height(heightSize.dp)
                        .onGloballyPositioned {
                            offsets[i] = it.boundsInParent().center.y
                        }
                )
            }
        }
    }
}