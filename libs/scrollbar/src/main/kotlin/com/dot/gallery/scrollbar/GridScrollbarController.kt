/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.scrollbar

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A single coalesced scroll request. The owning composable observes
 * [GridScrollbarController.scrollTarget] through a `snapshotFlow` + `collectLatest`
 * so that a fast thumb drag only ever commits the *latest* position instead of
 * queuing a `scrollToItem` per pointer event (the root cause of fast-scroll lag).
 */
@Stable
internal data class GridScrollTarget(val index: Int, val scrollOffset: Int)

/**
 * Remembers a [GridScrollbarController] bound to [state].
 *
 * Vertical-grid only by design: this is the exact path the Gallery timeline uses.
 */
@androidx.compose.runtime.Composable
internal fun rememberGridScrollbarController(
    state: LazyGridState,
    thumbMinLength: Float,
    thumbMaxLength: Float,
    alwaysShowScrollBar: Boolean,
    selectionMode: ScrollbarSelectionMode,
    snapIndices: IntArray,
    snapScrollOffset: Int,
    onSnap: () -> Unit,
): GridScrollbarController {
    val thumbMinLengthUpdated = rememberUpdatedState(thumbMinLength)
    val thumbMaxLengthUpdated = rememberUpdatedState(thumbMaxLength)
    val alwaysShowScrollBarUpdated = rememberUpdatedState(alwaysShowScrollBar)
    val selectionModeUpdated = rememberUpdatedState(selectionMode)
    val snapIndicesUpdated = rememberUpdatedState(snapIndices)
    val snapScrollOffsetUpdated = rememberUpdatedState(snapScrollOffset)
    val onSnapUpdated = rememberUpdatedState(onSnap)
    val reverseLayout = remember { derivedStateOf { state.layoutInfo.reverseLayout } }

    val isSelected = remember { mutableStateOf(false) }
    val dragOffset = remember { mutableFloatStateOf(0f) }
    val dragTargetIndex = remember { mutableIntStateOf(0) }

    val realFirstVisibleItem = remember {
        derivedStateOf {
            state.layoutInfo.visibleItemsInfo.firstOrNull {
                it.index == state.firstVisibleItemIndex
            }
        }
    }

    // LazyGridState doesn't expose the column count, so infer it from the first row.
    val nElementsMainAxis = remember {
        derivedStateOf {
            var count = 0
            for (item in state.layoutInfo.visibleItemsInfo) {
                val index = item.column
                if (index == -1) break
                if (count == index) count += 1 else break
            }
            count.coerceAtLeast(1)
        }
    }

    val isStickyHeaderInAction = remember {
        derivedStateOf {
            val realIndex = realFirstVisibleItem.value?.index ?: return@derivedStateOf false
            val firstVisibleIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                ?: return@derivedStateOf false
            realIndex != firstVisibleIndex
        }
    }

    // NOTE: upstream divided by size.width for the vertical axis (a latent bug that
    // only worked for square cells). We correctly use the height here.
    fun LazyGridItemInfo.fractionHiddenTop(firstItemOffset: Int): Float =
        if (size.height == 0) 0f else firstItemOffset / size.height.toFloat()

    fun LazyGridItemInfo.fractionVisibleBottom(viewportEndOffset: Int): Float =
        if (size.height == 0) 0f else (viewportEndOffset - offset.y).toFloat() / size.height.toFloat()

    val thumbSizeNormalizedReal = remember {
        derivedStateOf {
            state.layoutInfo.let {
                if (it.totalItemsCount == 0) return@let 0f
                val firstItem = realFirstVisibleItem.value ?: return@let 0f
                val firstPartial = firstItem.fractionHiddenTop(state.firstVisibleItemScrollOffset)
                val lastPartial =
                    1f - it.visibleItemsInfo.last().fractionVisibleBottom(it.viewportEndOffset)
                val rows = ceil(it.visibleItemsInfo.size.toFloat() / nElementsMainAxis.value) -
                        if (isStickyHeaderInAction.value) 1f else 0f
                val realVisibleSize = rows - firstPartial - lastPartial
                realVisibleSize / ceil(it.totalItemsCount.toFloat() / nElementsMainAxis.value)
            }
        }
    }

    val thumbSizeNormalized = remember {
        derivedStateOf {
            thumbSizeNormalizedReal.value.coerceIn(
                thumbMinLengthUpdated.value,
                thumbMaxLengthUpdated.value,
            )
        }
    }

    fun offsetCorrection(top: Float): Float {
        val topRealMax = (1f - thumbSizeNormalizedReal.value).coerceIn(0f, 1f)
        if (thumbSizeNormalizedReal.value >= thumbMinLengthUpdated.value) {
            return if (reverseLayout.value) topRealMax - top else top
        }
        val topMax = 1f - thumbMinLengthUpdated.value
        return if (reverseLayout.value) {
            (topRealMax - top) * topMax / topRealMax
        } else {
            top * topMax / topRealMax
        }
    }

    val layoutThumbOffsetNormalized = remember {
        derivedStateOf {
            state.layoutInfo.let {
                if (it.totalItemsCount == 0 || it.visibleItemsInfo.isEmpty()) return@let 0f
                val firstItem = realFirstVisibleItem.value ?: return@let 0f
                val top = firstItem.run {
                    ceil(index.toFloat() / nElementsMainAxis.value) +
                            fractionHiddenTop(state.firstVisibleItemScrollOffset)
                } / ceil(it.totalItemsCount.toFloat() / nElementsMainAxis.value)
                offsetCorrection(top)
            }
        }
    }

    // PERF: while dragging, drive the thumb straight from the drag offset so it tracks
    // the finger 1:1 instead of waiting for the list to lay out (visible fast-scroll lag).
    val thumbOffsetNormalized = remember {
        derivedStateOf {
            if (isSelected.value) {
                if (reverseLayout.value) {
                    (1f - thumbSizeNormalized.value - dragOffset.floatValue).coerceIn(0f, 1f)
                } else {
                    dragOffset.floatValue
                }
            } else {
                layoutThumbOffsetNormalized.value
            }
        }
    }

    val thumbIsInAction = remember {
        derivedStateOf {
            state.isScrollInProgress || isSelected.value || alwaysShowScrollBarUpdated.value
        }
    }

    return remember {
        GridScrollbarController(
            thumbSizeNormalized = thumbSizeNormalized,
            thumbSizeNormalizedReal = thumbSizeNormalizedReal,
            thumbOffsetNormalized = thumbOffsetNormalized,
            thumbIsInAction = thumbIsInAction,
            isSelected = isSelected,
            dragOffset = dragOffset,
            dragTargetIndex = dragTargetIndex,
            selectionMode = selectionModeUpdated,
            realFirstVisibleItem = realFirstVisibleItem,
            thumbMinLength = thumbMinLengthUpdated,
            reverseLayout = reverseLayout,
            nElementsMainAxis = nElementsMainAxis,
            snapIndices = snapIndicesUpdated,
            snapScrollOffset = snapScrollOffsetUpdated,
            onSnap = onSnapUpdated,
            state = state,
        )
    }
}

@Stable
internal class GridScrollbarController(
    val thumbSizeNormalized: State<Float>,
    val thumbOffsetNormalized: State<Float>,
    val thumbIsInAction: State<Boolean>,
    val isSelected: MutableState<Boolean>,
    private val thumbSizeNormalizedReal: State<Float>,
    private val dragOffset: MutableFloatState,
    private val dragTargetIndex: MutableIntState,
    private val selectionMode: State<ScrollbarSelectionMode>,
    private val realFirstVisibleItem: State<LazyGridItemInfo?>,
    private val thumbMinLength: State<Float>,
    private val reverseLayout: State<Boolean>,
    private val nElementsMainAxis: State<Int>,
    private val snapIndices: State<IntArray>,
    private val snapScrollOffset: State<Int>,
    private val onSnap: State<() -> Unit>,
    private val state: LazyGridState,
) {
    /** Month start the drag last snapped the grid to, used to fire one haptic per crossing. */
    private var lastSnappedIndex = -1
    /**
     * Latest coalesced scroll request, consumed by the owning composable via
     * `snapshotFlow { scrollTarget.value }.collectLatest { ... }`.
     */
    val scrollTarget: MutableState<GridScrollTarget?> = mutableStateOf(null)

    /**
     * The item index the indicator should describe. While dragging we return the
     * *intended* target (computed from the drag offset) so the label updates instantly
     * without waiting for the list to scroll/lay out; otherwise the real first item.
     */
    fun indicatorValue(): Int =
        if (isSelected.value) dragTargetIndex.intValue else state.firstVisibleItemIndex

    fun onDraggableState(deltaPixels: Float, maxLengthPixels: Float) {
        if (maxLengthPixels <= 0f) return
        val displace = if (reverseLayout.value) -deltaPixels else deltaPixels
        if (isSelected.value) {
            setScrollOffset(dragOffset.floatValue + displace / maxLengthPixels)
        }
    }

    fun onDragStarted(offsetPixels: Float, maxLengthPixels: Float) {
        if (maxLengthPixels <= 0f) return
        val newOffset = if (reverseLayout.value) {
            (maxLengthPixels - offsetPixels) / maxLengthPixels
        } else {
            offsetPixels / maxLengthPixels
        }
        val currentOffset = if (reverseLayout.value) {
            1f - thumbOffsetNormalized.value - thumbSizeNormalized.value
        } else {
            thumbOffsetNormalized.value
        }

        when (selectionMode.value) {
            ScrollbarSelectionMode.Full -> {
                if (newOffset in currentOffset..(currentOffset + thumbSizeNormalized.value)) {
                    setDragOffset(currentOffset)
                } else {
                    setScrollOffset(newOffset)
                }
                isSelected.value = true
            }

            ScrollbarSelectionMode.Thumb -> {
                if (newOffset in currentOffset..(currentOffset + thumbSizeNormalized.value)) {
                    setDragOffset(currentOffset)
                    isSelected.value = true
                }
            }

            ScrollbarSelectionMode.Disabled -> Unit
        }

        // Seed the snap origin so the first month crossing (not the grab itself) triggers haptics.
        if (isSelected.value && snapIndices.value.isNotEmpty()) {
            lastSnappedIndex = monthStartAtOrBelow(state.firstVisibleItemIndex)
            dragTargetIndex.intValue = lastSnappedIndex
        }
    }

    fun onDragStopped() {
        isSelected.value = false
        scrollTarget.value = null
        lastSnappedIndex = -1
    }

    private fun setScrollOffset(newOffset: Float) {
        // Keep the thumb tracking the finger 1:1 regardless of snapping.
        setDragOffset(newOffset)
        val columns = nElementsMainAxis.value
        val totalItems = state.layoutInfo.totalItemsCount
        val totalRows = ceil(totalItems.toFloat() / columns)
        val exactIndex = offsetCorrectionInverse(totalRows * dragOffset.floatValue)
        val continuousIndex = (floor(exactIndex).toInt() * columns)
            .coerceIn(0, (totalItems - 1).coerceAtLeast(0))

        // Month-snapping mode: jump the grid to the start of the month under the thumb and
        // commit a scroll (plus one haptic) only when the thumb crosses into a different month.
        if (snapIndices.value.isNotEmpty()) {
            val snapIndex = monthStartAtOrBelow(continuousIndex)
            dragTargetIndex.intValue = snapIndex
            if (snapIndex != lastSnappedIndex) {
                lastSnappedIndex = snapIndex
                scrollTarget.value = GridScrollTarget(index = snapIndex, scrollOffset = snapScrollOffset.value)
                onSnap.value()
            }
            return
        }

        val remainder = exactIndex - floor(exactIndex)
        val rowHeight = realFirstVisibleItem.value?.size?.height ?: 0
        val scrollOffset = (rowHeight * remainder).toInt()

        dragTargetIndex.intValue = continuousIndex
        scrollTarget.value = GridScrollTarget(index = continuousIndex, scrollOffset = scrollOffset)
    }

    /** Greatest month-start index that is &lt;= [target] (the month the thumb currently points at). */
    private fun monthStartAtOrBelow(target: Int): Int {
        val snaps = snapIndices.value
        if (snaps.isEmpty()) return target
        var lo = 0
        var hi = snaps.size - 1
        var result = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (snaps[mid] <= target) {
                result = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return snaps[result]
    }

    private fun setDragOffset(value: Float) {
        val maxValue = (1f - thumbSizeNormalized.value).coerceAtLeast(0f)
        dragOffset.floatValue = value.coerceIn(0f, maxValue)
    }

    private fun offsetCorrectionInverse(top: Float): Float {
        if (thumbSizeNormalizedReal.value >= thumbMinLength.value) return top
        val topRealMax = 1f - thumbSizeNormalizedReal.value
        val topMax = 1f - thumbMinLength.value
        return top * topRealMax / topMax
    }
}
