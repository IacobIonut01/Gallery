/*
 * Copyright 2023 usuiat
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

/**
 * [DoubleTapZoomSpec] defines a specification of zooming when double tap is detected.
 */
interface DoubleTapZoomSpec {

    /**
     * Determines the next scale value from the current scale value.
     *
     * The [ZoomState] object will call this function and pass the current scale value to
     * [currentScale] when a double tap gesture is detected.
     *
     * @param currentScale The current scale value.
     * @return The next scale value. The [ZoomState] object will change the scale to this value.
     */
    fun nextScale(currentScale: Float): Float

    companion object {
        /**
         * Disable double tap gesture.
         */
        val Disable: DoubleTapZoomSpec = object : DoubleTapZoomSpec {
            override fun nextScale(currentScale: Float) = currentScale
        }
    }
}

/**
 * Toggle the scale between 1.0 and [zoomScale] every time when double tap gesture is detected.
 *
 * When double tap gesture is detected,
 * - if the scale is 1.0, the scale will change to the [zoomScale].
 * - if the scale is not 1.0, the scale will change to 1.0.
 *
 * @param zoomScale The zoom scale value after double tap gesture is detected.
 */
class DoubleTapZoomScale(private val zoomScale: Float): DoubleTapZoomSpec {
    override fun nextScale(currentScale: Float): Float {
        return if (currentScale == 1f) zoomScale else 1f
    }
}

/**
 * Adopt the scale defined in the [scaleList] every time when double tap gesture is detected.
 *
 * @param scaleList The zoom scale values. The values must be defined in order of ascending.
 */
class DoubleTapZoomScaleList(private val scaleList: List<Float>): DoubleTapZoomSpec {
    override fun nextScale(currentScale: Float): Float {
        if (scaleList.isEmpty()) {
            return currentScale
        }

        for (scale in scaleList) {
            if (currentScale < scale) {
                return scale
            }
        }

        return scaleList[0]
    }
}