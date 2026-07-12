package com.dot.gallery.feature_node.domain.model.editor

/**
 * How a markup stroke paints. [Solid] uses the picked color (pen/highlighter/marker),
 * while [Blur] and [Mosaic] reveal a processed (blurred / pixelated) copy of the underlying
 * image along the stroke, used to obscure sensitive regions before sharing.
 */
enum class MarkupBrush {
    Solid, Blur, Mosaic
}
