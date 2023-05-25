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

val Icons.Face: ImageVector
    get() {
        if (_face != null) {
            return _face!!
        }
        _face = Builder(name = "Face", defaultWidth = 139.0.dp, defaultHeight = 139.0.dp,
                viewportWidth = 139.0f, viewportHeight = 139.0f).apply {
            path(fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveTo(64.887f, 137.975f)
                curveTo(67.81f, 139.342f, 71.19f, 139.342f, 74.113f, 137.975f)
                lineTo(84.568f, 133.086f)
                curveTo(85.726f, 132.544f, 86.97f, 132.211f, 88.244f, 132.101f)
                lineTo(99.742f, 131.107f)
                curveTo(102.958f, 130.83f, 105.884f, 129.14f, 107.732f, 126.494f)
                lineTo(114.342f, 117.033f)
                curveTo(115.074f, 115.985f, 115.985f, 115.074f, 117.033f, 114.342f)
                lineTo(126.494f, 107.732f)
                curveTo(129.14f, 105.884f, 130.83f, 102.958f, 131.107f, 99.742f)
                lineTo(132.101f, 88.244f)
                curveTo(132.211f, 86.97f, 132.544f, 85.726f, 133.086f, 84.568f)
                lineTo(137.975f, 74.113f)
                curveTo(139.342f, 71.19f, 139.342f, 67.81f, 137.975f, 64.887f)
                lineTo(133.086f, 54.432f)
                curveTo(132.544f, 53.274f, 132.211f, 52.03f, 132.101f, 50.756f)
                lineTo(131.107f, 39.258f)
                curveTo(130.83f, 36.042f, 129.14f, 33.116f, 126.494f, 31.268f)
                lineTo(117.033f, 24.658f)
                curveTo(115.985f, 23.926f, 115.074f, 23.015f, 114.342f, 21.967f)
                lineTo(107.732f, 12.506f)
                curveTo(105.884f, 9.86f, 102.958f, 8.17f, 99.742f, 7.893f)
                lineTo(88.244f, 6.899f)
                curveTo(86.97f, 6.789f, 85.726f, 6.456f, 84.568f, 5.914f)
                lineTo(74.113f, 1.025f)
                curveTo(71.19f, -0.342f, 67.81f, -0.342f, 64.887f, 1.025f)
                lineTo(54.432f, 5.914f)
                curveTo(53.274f, 6.456f, 52.03f, 6.789f, 50.756f, 6.899f)
                lineTo(39.258f, 7.893f)
                curveTo(36.042f, 8.17f, 33.116f, 9.86f, 31.268f, 12.506f)
                lineTo(24.658f, 21.967f)
                curveTo(23.926f, 23.015f, 23.015f, 23.926f, 21.967f, 24.658f)
                lineTo(12.506f, 31.268f)
                curveTo(9.86f, 33.116f, 8.17f, 36.042f, 7.893f, 39.258f)
                lineTo(6.899f, 50.756f)
                curveTo(6.789f, 52.03f, 6.456f, 53.274f, 5.914f, 54.432f)
                lineTo(1.025f, 64.887f)
                curveTo(-0.342f, 67.81f, -0.342f, 71.19f, 1.025f, 74.113f)
                lineTo(5.914f, 84.568f)
                curveTo(6.456f, 85.726f, 6.789f, 86.97f, 6.899f, 88.244f)
                lineTo(7.893f, 99.742f)
                curveTo(8.17f, 102.958f, 9.86f, 105.884f, 12.506f, 107.732f)
                lineTo(21.967f, 114.342f)
                curveTo(23.015f, 115.074f, 23.926f, 115.985f, 24.658f, 117.033f)
                lineTo(31.268f, 126.494f)
                curveTo(33.116f, 129.14f, 36.042f, 130.83f, 39.258f, 131.107f)
                lineTo(50.756f, 132.101f)
                curveTo(52.03f, 132.211f, 53.274f, 132.544f, 54.432f, 133.086f)
                lineTo(64.887f, 137.975f)
                close()
            }
        }
        .build()
        return _face!!
    }

private var _face: ImageVector? = null
