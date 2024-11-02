package com.dot.gallery.feature_node.presentation.edit.components.markup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dot.gallery.feature_node.domain.model.editor.PathProperties

@Composable
fun MarkupPathPreview(
    modifier: Modifier = Modifier,
    pathProperties: PathProperties,
    isSupportingPanel: Boolean
) {
    Canvas(
        modifier = modifier.padding(16.dp).size(40.dp)
    ) {
        val path = Path()
        if (isSupportingPanel) {
            path.moveTo(size.width / 2, 0f)
            path.lineTo(size.width / 2, size.height)
        } else {
            path.moveTo(0f, size.height / 2)
            path.lineTo(size.width, size.height / 2)
        }

        drawPath(
            color = pathProperties.color,
            path = path,
            style = Stroke(
                width = pathProperties.strokeWidth,
                cap = pathProperties.strokeCap,
                join = pathProperties.strokeJoin
            )
        )
    }
}