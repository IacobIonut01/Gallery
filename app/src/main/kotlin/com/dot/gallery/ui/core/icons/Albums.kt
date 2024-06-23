package com.dot.gallery.ui.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.dot.gallery.ui.core.Icons

public val Icons.Albums: ImageVector
    get() {
        if (_albums != null) {
            return _albums!!
        }
        _albums = Builder(name = "Albums", defaultWidth = 25.0.dp, defaultHeight = 24.0.dp,
                viewportWidth = 25.0f, viewportHeight = 24.0f).apply {
            path(fill = SolidColor(Color(0xFF49454F)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveTo(3.6665f, 11.0f)
                verticalLineTo(19.0f)
                horizontalLineTo(13.6665f)
                verticalLineTo(21.0f)
                horizontalLineTo(3.6665f)
                curveTo(2.5619f, 21.0f, 1.6665f, 20.1046f, 1.6665f, 19.0f)
                verticalLineTo(11.0f)
                curveTo(1.6665f, 9.8954f, 2.5619f, 9.0f, 3.6665f, 9.0f)
                horizontalLineTo(7.6665f)
                verticalLineTo(11.0f)
                horizontalLineTo(3.6665f)
                close()
            }
            path(fill = SolidColor(Color(0xFF49454F)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveTo(17.6665f, 5.0f)
                horizontalLineTo(9.6665f)
                lineTo(9.6665f, 21.0f)
                horizontalLineTo(7.6665f)
                verticalLineTo(5.0f)
                curveTo(7.6665f, 3.8954f, 8.5619f, 3.0f, 9.6665f, 3.0f)
                horizontalLineTo(17.6665f)
                curveTo(18.7711f, 3.0f, 19.6665f, 3.8954f, 19.6665f, 5.0f)
                verticalLineTo(13.0f)
                horizontalLineTo(17.6665f)
                verticalLineTo(5.0f)
                close()
            }
            path(fill = SolidColor(Color(0xFF49454F)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = EvenOdd) {
                moveTo(21.6665f, 15.0f)
                horizontalLineTo(13.6665f)
                lineTo(13.6665f, 19.0f)
                horizontalLineTo(21.6665f)
                verticalLineTo(15.0f)
                close()
                moveTo(13.6665f, 13.0f)
                curveTo(12.5619f, 13.0f, 11.6665f, 13.8954f, 11.6665f, 15.0f)
                verticalLineTo(19.0f)
                curveTo(11.6665f, 20.1046f, 12.5619f, 21.0f, 13.6665f, 21.0f)
                horizontalLineTo(21.6665f)
                curveTo(22.7711f, 21.0f, 23.6665f, 20.1046f, 23.6665f, 19.0f)
                verticalLineTo(15.0f)
                curveTo(23.6665f, 13.8954f, 22.7711f, 13.0f, 21.6665f, 13.0f)
                horizontalLineTo(13.6665f)
                close()
            }
        }
        .build()
        return _albums!!
    }

private var _albums: ImageVector? = null
