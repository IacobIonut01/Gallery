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

val Icons.NoImage: ImageVector
    get() {
        if (_noImage != null) {
            return _noImage!!
        }
        _noImage = Builder(
            name = "NoImage", defaultWidth = 128.0.dp, defaultHeight = 128.0.dp,
            viewportWidth = 128.0f, viewportHeight = 128.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xff000000)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(120.0f, 13.656f)
                lineTo(114.344f, 8.0f)
                lineTo(8.0f, 114.344f)
                lineTo(13.656f, 120.0f)
                lineTo(21.656f, 112.0f)
                horizontalLineTo(104.0f)
                curveTo(106.121f, 111.997f, 108.154f, 111.153f, 109.653f, 109.653f)
                curveTo(111.153f, 108.154f, 111.997f, 106.121f, 112.0f, 104.0f)
                verticalLineTo(21.656f)
                lineTo(120.0f, 13.656f)
                close()
                moveTo(104.0f, 104.0f)
                horizontalLineTo(29.656f)
                lineTo(60.828f, 72.828f)
                lineTo(70.344f, 82.344f)
                curveTo(71.844f, 83.844f, 73.879f, 84.686f, 76.0f, 84.686f)
                curveTo(78.121f, 84.686f, 80.156f, 83.844f, 81.656f, 82.344f)
                lineTo(88.0f, 76.0f)
                lineTo(104.0f, 91.988f)
                verticalLineTo(104.0f)
                close()
                moveTo(104.0f, 80.672f)
                lineTo(93.656f, 70.328f)
                curveTo(92.156f, 68.828f, 90.121f, 67.986f, 88.0f, 67.986f)
                curveTo(85.879f, 67.986f, 83.844f, 68.828f, 82.344f, 70.328f)
                lineTo(76.0f, 76.672f)
                lineTo(66.492f, 67.164f)
                lineTo(104.0f, 29.656f)
                verticalLineTo(80.672f)
                close()
                moveTo(24.0f, 88.0f)
                verticalLineTo(76.0f)
                lineTo(44.0f, 56.012f)
                lineTo(49.492f, 61.508f)
                lineTo(55.156f, 55.844f)
                lineTo(49.656f, 50.344f)
                curveTo(48.156f, 48.844f, 46.121f, 48.002f, 44.0f, 48.002f)
                curveTo(41.879f, 48.002f, 39.844f, 48.844f, 38.344f, 50.344f)
                lineTo(24.0f, 64.688f)
                verticalLineTo(24.0f)
                horizontalLineTo(88.0f)
                verticalLineTo(16.0f)
                horizontalLineTo(24.0f)
                curveTo(21.879f, 16.002f, 19.845f, 16.846f, 18.346f, 18.346f)
                curveTo(16.846f, 19.845f, 16.002f, 21.879f, 16.0f, 24.0f)
                verticalLineTo(88.0f)
                horizontalLineTo(24.0f)
                close()
            }
        }
            .build()
        return _noImage!!
    }

private var _noImage: ImageVector? = null
