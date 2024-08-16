package com.dot.gallery.feature_node.presentation.edit.components.utils.shapes

import android.graphics.Canvas
import android.graphics.Paint

interface Shape {
    fun draw(canvas: Canvas, paint: Paint)
    fun startShape(x: Float, y: Float)
    fun moveShape(x: Float, y: Float)
    fun stopShape()
}