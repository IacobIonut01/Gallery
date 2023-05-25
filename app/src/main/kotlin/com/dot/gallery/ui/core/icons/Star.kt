package com.dot.gallery.ui.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.dot.gallery.ui.core.Icons

val Icons.Star: ImageVector
    get() {
        if (star != null) {
            return star!!
        }
        star = Builder(name = "Star", defaultWidth = 167.0.dp, defaultHeight = 139.0.dp,
                viewportWidth = 167.0f, viewportHeight = 139.0f).apply {
            path(fill = SolidColor(Color(0xff000000)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveTo(3.691f, 50.274f)
                curveTo(-11.842f, 20.417f, 24.458f, -9.886f, 60.224f, 3.081f)
                lineTo(66.137f, 5.225f)
                curveTo(77.057f, 9.184f, 89.454f, 9.184f, 100.373f, 5.225f)
                lineTo(106.287f, 3.081f)
                curveTo(142.052f, -9.886f, 178.353f, 20.417f, 162.819f, 50.274f)
                lineTo(160.251f, 55.21f)
                curveTo(155.509f, 64.325f, 155.509f, 74.675f, 160.251f, 83.79f)
                lineTo(162.819f, 88.726f)
                curveTo(178.353f, 118.583f, 142.052f, 148.886f, 106.287f, 135.919f)
                lineTo(100.373f, 133.775f)
                curveTo(89.454f, 129.816f, 77.057f, 129.816f, 66.137f, 133.775f)
                lineTo(60.224f, 135.919f)
                curveTo(24.458f, 148.886f, -11.842f, 118.583f, 3.691f, 88.726f)
                lineTo(6.259f, 83.79f)
                curveTo(11.002f, 74.675f, 11.002f, 64.325f, 6.259f, 55.21f)
                lineTo(3.691f, 50.274f)
                close()
            }
        }
        .build()
        return star!!
    }

private var star: ImageVector? = null
