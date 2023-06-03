/*
 * Copyright 2022 usuiat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dot.gallery.core.util.zoomable

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.Float.max
import kotlin.math.abs

/**
 * A state object that manage scale and offset.
 *
 * @param maxScale The maximum scale of the content.
 * @param contentSize Size of content (i.e. image size.) If Zero, the composable layout size will
 * be used as content size.
 * @param velocityDecay The decay animation spec for fling behaviour.
 */
@Stable
class ZoomState(
    @FloatRange(from = 1.0) private val maxScale: Float = 5f,
    private var contentSize: Size = Size.Zero,
    private val velocityDecay: DecayAnimationSpec<Float> = exponentialDecay(),
) {
    init {
        require(maxScale >= 1.0f) { "maxScale must be at least 1.0." }
    }

    private var _scale = Animatable(1f).apply {
        updateBounds(0.9f, maxScale)
    }
    /**
     * The scale of the content.
     */
    val scale: Float
        get() = _scale.value

    private var _offsetX = Animatable(0f)
    /**
     * The horizontal offset of the content.
     */
    val offsetX: Float
        get() = _offsetX.value

    private var _offsetY = Animatable(0f)
    /**
     * The vertical offset of the content.
     */
    val offsetY: Float
        get() = _offsetY.value

    private var layoutSize = Size.Zero
    /**
     * Set composable layout size.
     *
     * Basically This function is called from [Modifier.zoomable] only.
     *
     * @param size The size of composable layout size.
     */
    fun setLayoutSize(size: Size) {
        layoutSize = size
        updateFitContentSize()
    }

    /**
     * Set the content size.
     *
     * @param size The content size, for example an image size in pixel.
     */
    fun setContentSize(size: Size) {
        contentSize = size
        updateFitContentSize()
    }

    private var fitContentSize = Size.Zero
    private fun updateFitContentSize() {
        if (layoutSize == Size.Zero) {
            fitContentSize = Size.Zero
            return
        }

        if (contentSize == Size.Zero) {
            fitContentSize = layoutSize
            return
        }

        val contentAspectRatio = contentSize.width / contentSize.height
        val layoutAspectRatio = layoutSize.width / layoutSize.height

        fitContentSize = if (contentAspectRatio > layoutAspectRatio) {
            contentSize * (layoutSize.width / contentSize.width)
        } else {
            contentSize * (layoutSize.height / contentSize.height)
        }
    }

    /**
     * Reset the scale and the offsets.
     */
    suspend fun reset() = coroutineScope {
        launch { _scale.snapTo(1f) }
        _offsetX.updateBounds(0f, 0f)
        launch { _offsetX.snapTo(0f) }
        _offsetY.updateBounds(0f, 0f)
        launch { _offsetY.snapTo(0f) }
    }

    private var shouldConsumeEvent: Boolean? = null

    internal fun startGesture() {
        shouldConsumeEvent = null
    }

    internal fun canConsumeGesture(pan: Offset, zoom: Float): Boolean {
        return shouldConsumeEvent ?: run {
            var consume = true
            if (zoom == 1f) { // One finger gesture
                if (scale == 1f) {  // Not zoomed
                    consume = false
                } else {
                    val ratio = (abs(pan.x) / abs(pan.y))
                    if (ratio > 3) {   // Horizontal drag
                        if ((pan.x < 0) && (_offsetX.value == _offsetX.lowerBound)) {
                            // Drag R to L when right edge of the content is shown.
                            consume = false
                        }
                        if ((pan.x > 0) && (_offsetX.value == _offsetX.upperBound)) {
                            // Drag L to R when left edge of the content is shown.
                            consume = false
                        }
                    }
                    else if (ratio < 0.33) { // Vertical drag
                        if ((pan.y < 0) && (_offsetY.value == _offsetY.lowerBound)) {
                            // Drag bottom to top when bottom edge of the content is shown.
                            consume = false
                        }
                        if ((pan.y > 0) && (_offsetY.value == _offsetY.upperBound)) {
                            // Drag top to bottom when top edge of the content is shown.
                            consume = false
                        }
                    }
                }
            }
            shouldConsumeEvent = consume
            consume
        }
    }

    private val velocityTracker = VelocityTracker()
    private var shouldFling = true

    internal suspend fun applyGesture(
        pan: Offset,
        zoom: Float,
        position: Offset,
        timeMillis: Long
    ) = coroutineScope {

        val newScale = (scale * zoom).coerceIn(0.9f, maxScale)

        val newOffset = calculateNewOffset(newScale, position, pan)
        val newBounds = calculateNewBounds(newScale)

        _offsetX.updateBounds(newBounds.left, newBounds.right)
        launch {
            _offsetX.snapTo(newOffset.x)
        }

        _offsetY.updateBounds(newBounds.top, newBounds.bottom)
        launch {
            _offsetY.snapTo(newOffset.y)
        }

        launch {
            _scale.snapTo(newScale)
        }

        velocityTracker.addPosition(timeMillis, position)

        if (zoom != 1f) {
            shouldFling = false
        }
    }

    internal suspend fun changeScale(
        targetScale: Float,
        position: Offset,
        animationSpec: AnimationSpec<Float> = spring(),
    ) = coroutineScope {
        val newScale = targetScale.coerceIn(1f, maxScale)
        val newOffset = calculateNewOffset(newScale, position, Offset.Zero)
        val newBounds = calculateNewBounds(newScale)

        val x = newOffset.x.coerceIn(newBounds.left, newBounds.right)
        launch {
            _offsetX.updateBounds(null, null)
            _offsetX.animateTo(x, animationSpec)
            _offsetX.updateBounds(newBounds.left, newBounds.right)
        }

        val y = newOffset.y.coerceIn(newBounds.top, newBounds.bottom)
        launch {
            _offsetY.updateBounds(null, null)
            _offsetY.animateTo(y, animationSpec)
            _offsetY.updateBounds(newBounds.top, newBounds.bottom)
        }

        launch {
            _scale.animateTo(newScale, animationSpec)
        }

        shouldFling = false
    }

    private fun calculateNewOffset(
        newScale: Float,
        position: Offset,
        pan: Offset,
    ): Offset {
        val size = fitContentSize * scale
        val newSize = fitContentSize * newScale
        val deltaWidth = newSize.width - size.width
        val deltaHeight = newSize.height - size.height

        // Position with the origin at the left top corner of the content.
        val xInContent = position.x - offsetX + (size.width - layoutSize.width) * 0.5f
        val yInContent = position.y - offsetY + (size.height - layoutSize.height) * 0.5f
        // Amount of offset change required to zoom around the position.
        val deltaX = (deltaWidth * 0.5f) - (deltaWidth * xInContent / size.width)
        val deltaY = (deltaHeight * 0.5f) - (deltaHeight * yInContent / size.height)

        val x = offsetX + pan.x + deltaX
        val y = offsetY + pan.y + deltaY

        return Offset(x, y)
    }

    private fun calculateNewBounds(
        newScale: Float,
    ): Rect {
        val newSize = fitContentSize * newScale
        val boundX = max((newSize.width - layoutSize.width), 0f) * 0.5f
        val boundY = max((newSize.height - layoutSize.height), 0f) * 0.5f
        return Rect(-boundX, -boundY, boundX, boundY)
    }

    internal suspend fun endGesture() = coroutineScope {
        if (shouldFling) {
            val velocity = velocityTracker.calculateVelocity()
            launch {
                _offsetX.animateDecay(velocity.x, velocityDecay)
            }
            launch {
                _offsetY.animateDecay(velocity.y, velocityDecay)
            }
        }
        shouldFling = true

        if (_scale.value < 1f) {
            launch {
                _scale.animateTo(1f)
            }
        }
    }
}

/**
 * Creates a [ZoomState] that is remembered across compositions.
 *
 * @param maxScale The maximum scale of the content.
 * @param contentSize Size of content (i.e. image size.) If Zero, the composable layout size will
 * be used as content size.
 * @param velocityDecay The decay animation spec for fling behaviour.
 */
@Composable
fun rememberZoomState(
    @FloatRange(from = 1.0) maxScale: Float = 5f,
    contentSize: Size = Size.Zero,
    velocityDecay: DecayAnimationSpec<Float> = exponentialDecay(),
) = remember {
    ZoomState(maxScale, contentSize, velocityDecay)
}