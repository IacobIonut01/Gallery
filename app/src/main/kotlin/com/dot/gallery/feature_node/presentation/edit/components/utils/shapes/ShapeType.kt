package com.dot.gallery.feature_node.presentation.edit.components.utils.shapes

/**
 * The different kind of known Shapes.
 */
sealed interface ShapeType {

    data object Brush : ShapeType
    data object Oval : ShapeType
    data object Rectangle : ShapeType
    data object Line : ShapeType
    class Arrow(val pointerLocation: ArrowPointerLocation = ArrowPointerLocation.START) : ShapeType

}