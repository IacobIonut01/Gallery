/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.scrollbar

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs
import kotlin.math.floor

/**
 * A single coalesced scroll request. The owning composable observes
 * [GridScrollbarController.scrollTarget] through a `snapshotFlow` + `collectLatest`
 * so that a fast thumb drag only ever commits the *latest* position instead of
 * queuing a `scrollToItem` per pointer event (the root cause of fast-scroll lag).
 */
@Stable
internal data class GridScrollTarget(val index: Int, val scrollOffset: Int)

/** One visible grid item, reduced to what the scroll geometry needs. */
@Stable
internal data class GridScrollItem(val index: Int, val offsetY: Int, val height: Int)

/** One layout pass of the grid, reduced to what the scroll geometry needs. */
@Stable
internal data class GridScrollFrame(
    val totalItems: Int,
    val viewportPixels: Int,
    val firstVisibleIndex: Int,
    val firstVisibleOffset: Int,
    val items: List<GridScrollItem>,
)

/**
 * Distance the content has scrolled, in pixels, measured from where it was last anchored.
 *
 * A lazy grid exposes no absolute pixel scroll offset, and an item index cannot stand in for one:
 * a mosaic lays two to four items in a single row, so the index jumps by that many while the
 * content moves exactly one row — which is why a thumb derived from it skipped and stuttered.
 * The items' own viewport offsets are pixel-accurate, and the difference of one item's offset
 * between two layout passes is exactly how far the content moved.
 */
internal class GridScrollPixelTracker {
    private var previous = emptyList<GridScrollItem>()

    /** Content pixels scrolled since the last anchor. */
    var pixels = 0f
        private set

    /**
     * True when the last update carried its position over from an item instead of re-anchoring.
     * A re-anchor seeds the distance from an estimate, so an interval containing one says nothing
     * about how far the content really moved.
     */
    var carriedOver = false
        private set

    /**
     * Folds one layout pass in and returns the updated distance.
     *
     * An item that is visible now and was visible in the previous pass carries the position:
     * every item that scrolls moves by the same amount, so the handover between items is lossless
     * and needs no estimate. When nothing carried over — the first pass, or passes a fast fling
     * skipped entirely — the distance advances by [indexIncrementPixels], the items that went by
     * at the measured distance per item, and only the very first pass is seeded absolutely from
     * [seedPixels]. Advancing rather than re-seeding keeps a skipped pass from moving the thumb.
     *
     * The carried item is taken from the middle of the viewport on purpose: a sticky header is
     * pinned to the top edge and does not move with the content at all, so anchoring on whatever
     * happens to come first would under-count the distance by the header's share of the scroll.
     */
    fun update(
        items: List<GridScrollItem>,
        viewportPixels: Int,
        seedPixels: Float,
        indexIncrementPixels: Float,
    ): Float {
        val viewportMiddle = viewportPixels / 2
        val carried = items
            .filter { item -> previous.any { it.index == item.index } }
            .minByOrNull { item -> abs(item.offsetY - viewportMiddle) }
        carriedOver = carried != null
        if (carried != null) {
            val before = previous.first { it.index == carried.index }
            pixels += (before.offsetY - carried.offsetY).toFloat()
        } else if (previous.isEmpty()) {
            pixels = seedPixels
        } else {
            pixels += indexIncrementPixels
        }
        previous = items
        return pixels
    }

    /** Anchors the distance at a known absolute position (the top of the list reads 0). */
    fun resetTo(pixels: Float): Float {
        this.pixels = pixels
        return pixels
    }
}

/**
 * The item under the viewport's top edge, as a scroll position: the index of the first visible
 * item plus [fractionIntoItem], how much of it has been scrolled past (0f..1f).
 */
internal fun scrollbarItemsAbove(firstVisibleItemIndex: Int, fractionIntoItem: Float): Float =
    firstVisibleItemIndex + fractionIntoItem.coerceIn(0f, 1f)

/**
 * Height of the whole content in pixels: [pixelsPerItem], the distance the content moves per item
 * passed, over every item.
 *
 * Deliberately *not* the average tile height: a grid lays several items in one row, so an item
 * occupies a fraction of a row and the content is far shorter than the items stacked. The
 * distance per passed item has no such factor in it, and it is measured rather than inferred.
 */
internal fun scrollbarContentPixels(pixelsPerItem: Float, totalItems: Int): Float =
    if (pixelsPerItem <= 0f || totalItems <= 0) 0f else pixelsPerItem * totalItems

/** Share of the content the viewport shows (0f..1f), which sizes the thumb. */
internal fun scrollbarVisibleShare(visiblePixels: Float, contentPixels: Float): Float =
    if (contentPixels <= 0f) 0f else (visiblePixels / contentPixels).coerceIn(0f, 1f)

/**
 * How far through the scrollable range the viewport is (0f..1f): the content scrolled over the
 * distance the content *can* scroll (its height minus one viewport). Measured in pixels, so the
 * thumb advances at the speed of the content instead of at the speed of an item index.
 */
internal fun scrollbarProgress(
    scrollPixels: Float,
    contentPixels: Float,
    visiblePixels: Float,
): Float {
    val scrollable = contentPixels - visiblePixels
    if (scrollable <= 0f) return if (scrollPixels > 0f) 1f else 0f
    return (scrollPixels / scrollable).coerceIn(0f, 1f)
}

/**
 * Where the thumb's top edge goes on the track: the content's progress mapped onto the distance
 * the thumb itself can travel. No correction term is needed — with the share measured in the
 * same pixels as the position, progress 1f lands the thumb's bottom edge exactly at the end.
 */
internal fun scrollbarThumbTop(progress: Float, displayedShare: Float): Float =
    progress.coerceIn(0f, 1f) * (1f - displayedShare.coerceIn(0f, 1f))

/** Content pixels a thumb position points at — the inverse of [scrollbarThumbTop]. */
internal fun scrollbarPixelsAtThumbTop(
    thumbTop: Float,
    displayedShare: Float,
    scrollablePixels: Float,
): Float {
    val travel = 1f - displayedShare.coerceIn(0f, 1f)
    if (travel <= 0f) return 0f
    return (thumbTop / travel).coerceIn(0f, 1f) * scrollablePixels
}

/**
 * Fraction of the remaining difference the geometry's content height covers per layout pass. The
 * estimate is corrected whenever the end of the list is reached and it refines itself while the
 * list is scrolled; easing every step keeps a correction from moving the thumb in a single frame.
 */
private const val CONTENT_HEIGHT_EASING = 0.06f

/**
 * Remembers a [GridScrollbarController] bound to [state].
 *
 * Vertical-grid only by design: this is the exact path the Gallery timeline uses.
 */
@Composable
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

    // Scroll position in content pixels, and the distance the content moves per item passed,
    // which turns an item count into a content height and back into an item index for a drag.
    val scrollPixels = remember { mutableFloatStateOf(0f) }
    val pixelsPerItem = remember { mutableFloatStateOf(0f) }
    // Content height measured at an end of the list, where it can be solved exactly; 0 while the
    // estimate stands. Cleared when the list itself changes.
    val calibratedContentPixels = remember { mutableFloatStateOf(0f) }
    val calibratedTotalItems = remember { mutableIntStateOf(0) }
    val contentPixelsTarget = remember {
        derivedStateOf {
            calibratedContentPixels.floatValue.takeIf { it > 0f }
                ?: scrollbarContentPixels(
                    pixelsPerItem = pixelsPerItem.floatValue,
                    totalItems = state.layoutInfo.totalItemsCount,
                )
        }
    }
    // The height the geometry actually uses: it eases towards the target instead of stepping to it,
    // so a correction — the end of the list being reached, the first measurement replacing the
    // initial guess — glides the thumb along instead of jumping it in one frame.
    val smoothedContentPixels = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state) {
        val tracker = GridScrollPixelTracker()
        // Accumulated distance and items passed, from which the scale is measured.
        var measuredPixels = 0f
        var measuredItems = 0f
        var baselinePixels = 0f
        var baselineIndex = -1
        var intervalIsClean = true
        var lastSeenIndex = -1
        snapshotFlow {
            state.layoutInfo.let { info ->
                GridScrollFrame(
                    totalItems = info.totalItemsCount,
                    viewportPixels = info.viewportEndOffset - info.viewportStartOffset,
                    firstVisibleIndex = state.firstVisibleItemIndex,
                    firstVisibleOffset = state.firstVisibleItemScrollOffset,
                    items = info.visibleItemsInfo.map {
                        GridScrollItem(it.index, it.offset.y, it.size.height)
                    },
                )
            }
        }.collect { frame ->
            if (frame.totalItems == 0 || frame.items.isEmpty()) return@collect
            if (calibratedTotalItems.intValue != frame.totalItems) {
                calibratedTotalItems.intValue = frame.totalItems
                calibratedContentPixels.floatValue = 0f
            }
            val firstItem = frame.items.firstOrNull { it.index == frame.firstVisibleIndex }
                ?: frame.items.first()
            val fractionIntoItem = if (firstItem.height == 0) {
                0f
            } else {
                (frame.firstVisibleOffset / firstItem.height.toFloat()).coerceIn(0f, 1f)
            }
            if (measuredItems == 0f) {
                // Nothing passed yet, so nothing measured: spread the viewport over the items on
                // screen, which in a grid is the distance per item (one row of items covers one
                // row height) and close enough that the first measurement only refines it.
                pixelsPerItem.floatValue = frame.viewportPixels.toFloat() / frame.items.size
            }
            val seedPixels = scrollbarItemsAbove(
                firstVisibleItemIndex = firstItem.index,
                fractionIntoItem = fractionIntoItem,
            ) * pixelsPerItem.floatValue
            val indexIncrement = if (lastSeenIndex < 0) {
                0f
            } else {
                (frame.firstVisibleIndex - lastSeenIndex) * pixelsPerItem.floatValue
            }
            lastSeenIndex = frame.firstVisibleIndex
            // The top of the list is an absolute reference the integration can be pinned to: a
            // first pass seeded from an estimate, or a jump into the middle, cannot know where it
            // is, so use the ends to correct the scale whenever they are reached.
            if (!state.canScrollBackward) {
                scrollPixels.floatValue = tracker.resetTo(0f)
            } else {
                scrollPixels.floatValue = tracker.update(
                    items = frame.items,
                    viewportPixels = frame.viewportPixels,
                    seedPixels = seedPixels,
                    indexIncrementPixels = indexIncrement,
                )
            }
            // Distance the content moves per item passed, measured between two item advances: it
            // is what turns an item count into a content height, and it is the only scale a grid
            // makes observable — the tile heights say nothing about how many items share a row.
            // Measured, so it converges as the list is scrolled instead of being inferred from
            // whichever tiles happen to be on screen.
            if (baselineIndex < 0) {
                baselineIndex = frame.firstVisibleIndex
                baselinePixels = scrollPixels.floatValue
                intervalIsClean = true
            } else if (frame.firstVisibleIndex != baselineIndex) {
                val passedItems = (frame.firstVisibleIndex - baselineIndex).toFloat()
                val passedPixels = scrollPixels.floatValue - baselinePixels
                // Matching signs mean the distance belongs to those items, and a clean interval
                // means no re-anchor seeded part of it from an estimate; a jump (a month snap, a
                // reload) leaves the two unrelated.
                if (intervalIsClean && passedItems * passedPixels > 0f) {
                    measuredPixels += passedPixels
                    measuredItems += passedItems
                    pixelsPerItem.floatValue = measuredPixels / measuredItems
                }
                baselineIndex = frame.firstVisibleIndex
                baselinePixels = scrollPixels.floatValue
                intervalIsClean = tracker.carriedOver
            } else if (!tracker.carriedOver) {
                intervalIsClean = false
            }
            if (!state.canScrollForward && state.canScrollBackward) {
                calibratedContentPixels.floatValue = scrollPixels.floatValue + frame.viewportPixels
            }
            // Ease the height the geometry uses towards the target: every correction the estimate
            // makes — the list end being reached, the first measurement replacing the guess —
            // moves the thumb along instead of jumping it.
            val contentTarget = contentPixelsTarget.value
            val contentNow = smoothedContentPixels.floatValue
            smoothedContentPixels.floatValue = if (contentNow <= 0f) {
                contentTarget
            } else {
                contentNow + (contentTarget - contentNow) * CONTENT_HEIGHT_EASING
            }
        }
    }

    val viewportPixels = remember {
        derivedStateOf {
            state.layoutInfo.let {
                (it.viewportEndOffset - it.viewportStartOffset).toFloat()
            }
        }
    }
    val contentPixels: State<Float> = remember { derivedStateOf { smoothedContentPixels.floatValue } }
    val thumbSizeNormalizedReal = remember {
        derivedStateOf {
            scrollbarVisibleShare(viewportPixels.value, contentPixels.value)
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

    val thumbProgress = remember {
        derivedStateOf {
            scrollbarProgress(
                scrollPixels = scrollPixels.floatValue,
                contentPixels = contentPixels.value,
                visiblePixels = viewportPixels.value,
            )
        }
    }

    val layoutThumbOffsetNormalized = remember {
        derivedStateOf {
            val displayed = thumbSizeNormalized.value
            val top = scrollbarThumbTop(thumbProgress.value, displayed)
            if (reverseLayout.value) (1f - displayed - top).coerceIn(0f, 1f) else top
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
            thumbOffsetNormalized = thumbOffsetNormalized,
            thumbIsInAction = thumbIsInAction,
            isSelected = isSelected,
            dragOffset = dragOffset,
            dragTargetIndex = dragTargetIndex,
            selectionMode = selectionModeUpdated,
            realFirstVisibleItem = realFirstVisibleItem,
            reverseLayout = reverseLayout,
            contentPixels = contentPixels,
            viewportPixels = viewportPixels,
            pixelsPerItem = pixelsPerItem,
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
    private val dragOffset: MutableFloatState,
    private val dragTargetIndex: MutableIntState,
    private val selectionMode: State<ScrollbarSelectionMode>,
    private val realFirstVisibleItem: State<LazyGridItemInfo?>,
    private val reverseLayout: State<Boolean>,
    private val contentPixels: State<Float>,
    private val viewportPixels: State<Float>,
    private val pixelsPerItem: State<Float>,
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
        val totalItems = state.layoutInfo.totalItemsCount
        val scrollablePixels = (contentPixels.value - viewportPixels.value).coerceAtLeast(0f)
        val targetPixels = scrollbarPixelsAtThumbTop(
            thumbTop = newOffset,
            displayedShare = thumbSizeNormalized.value,
            scrollablePixels = scrollablePixels,
        )
        val perItem = pixelsPerItem.value
        val itemsAbove = if (perItem > 0f) targetPixels / perItem else 0f
        val continuousIndex = floor(itemsAbove).toInt()
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

        val remainder = itemsAbove - floor(itemsAbove)
        val itemHeight = realFirstVisibleItem.value?.size?.height ?: 0
        val scrollOffset = (itemHeight * remainder).toInt()

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
}
