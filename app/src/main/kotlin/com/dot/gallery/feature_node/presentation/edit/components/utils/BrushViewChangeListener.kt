package com.dot.gallery.feature_node.presentation.edit.components.utils

import com.dot.gallery.feature_node.presentation.edit.components.views.DrawingView

/**
 * Created on 1/17/2018.
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 *
 *
 */
interface BrushViewChangeListener {
    fun onViewAdd(drawingView: DrawingView)
    fun onViewRemoved(drawingView: DrawingView)
    fun onStartDrawing()
    fun onStopDrawing()
}