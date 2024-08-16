package com.dot.gallery.ui.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.dot.gallery.ui.core.Icons

public val Icons.RegularExpression: ImageVector
    get() {
        if (_regularExpression != null) {
            return _regularExpression!!
        }
        _regularExpression = Builder(name = "RegularExpression", defaultWidth = 48.0.dp,
                defaultHeight = 48.0.dp, viewportWidth = 48.0f, viewportHeight = 48.0f).apply {
            group {
                path(fill = SolidColor(Color(0xFFD0BCFF)), stroke = null, strokeLineWidth = 0.0f,
                        strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                        pathFillType = NonZero) {
                    moveTo(9.85f, 38.05f)
                    curveTo(7.9833f, 36.15f, 6.5417f, 33.9833f, 5.525f, 31.55f)
                    curveTo(4.5083f, 29.1167f, 4.0f, 26.5667f, 4.0f, 23.9f)
                    curveTo(4.0f, 21.2333f, 4.5f, 18.6833f, 5.5f, 16.25f)
                    curveTo(6.5f, 13.8167f, 7.95f, 11.65f, 9.85f, 9.75f)
                    lineTo(12.7f, 12.6f)
                    curveTo(11.1667f, 14.1f, 10.0f, 15.825f, 9.2f, 17.775f)
                    curveTo(8.4f, 19.725f, 8.0f, 21.7667f, 8.0f, 23.9f)
                    curveTo(8.0f, 26.0333f, 8.4083f, 28.075f, 9.225f, 30.025f)
                    curveTo(10.0417f, 31.975f, 11.2f, 33.7f, 12.7f, 35.2f)
                    lineTo(9.85f, 38.05f)
                    close()
                    moveTo(19.0f, 36.0f)
                    curveTo(18.1667f, 36.0f, 17.4583f, 35.7083f, 16.875f, 35.125f)
                    curveTo(16.2917f, 34.5417f, 16.0f, 33.8333f, 16.0f, 33.0f)
                    curveTo(16.0f, 32.1667f, 16.2917f, 31.4583f, 16.875f, 30.875f)
                    curveTo(17.4583f, 30.2917f, 18.1667f, 30.0f, 19.0f, 30.0f)
                    curveTo(19.8333f, 30.0f, 20.5417f, 30.2917f, 21.125f, 30.875f)
                    curveTo(21.7083f, 31.4583f, 22.0f, 32.1667f, 22.0f, 33.0f)
                    curveTo(22.0f, 33.8333f, 21.7083f, 34.5417f, 21.125f, 35.125f)
                    curveTo(20.5417f, 35.7083f, 19.8333f, 36.0f, 19.0f, 36.0f)
                    close()
                    moveTo(25.95f, 26.0f)
                    verticalLineTo(22.45f)
                    lineTo(22.9f, 24.25f)
                    lineTo(20.9f, 20.75f)
                    lineTo(23.95f, 19.0f)
                    lineTo(20.9f, 17.25f)
                    lineTo(22.9f, 13.75f)
                    lineTo(25.95f, 15.55f)
                    verticalLineTo(12.0f)
                    horizontalLineTo(29.95f)
                    verticalLineTo(15.55f)
                    lineTo(33.0f, 13.75f)
                    lineTo(35.0f, 17.25f)
                    lineTo(31.95f, 19.0f)
                    lineTo(35.0f, 20.75f)
                    lineTo(33.0f, 24.25f)
                    lineTo(29.95f, 22.45f)
                    verticalLineTo(26.0f)
                    horizontalLineTo(25.95f)
                    close()
                    moveTo(38.15f, 38.05f)
                    lineTo(35.3f, 35.2f)
                    curveTo(36.8333f, 33.7f, 38.0f, 31.975f, 38.8f, 30.025f)
                    curveTo(39.6f, 28.075f, 40.0f, 26.0333f, 40.0f, 23.9f)
                    curveTo(40.0f, 21.7667f, 39.5917f, 19.725f, 38.775f, 17.775f)
                    curveTo(37.9583f, 15.825f, 36.8f, 14.1f, 35.3f, 12.6f)
                    lineTo(38.15f, 9.75f)
                    curveTo(40.0167f, 11.65f, 41.4583f, 13.8167f, 42.475f, 16.25f)
                    curveTo(43.4917f, 18.6833f, 44.0f, 21.2333f, 44.0f, 23.9f)
                    curveTo(44.0f, 26.5667f, 43.5f, 29.1167f, 42.5f, 31.55f)
                    curveTo(41.5f, 33.9833f, 40.05f, 36.15f, 38.15f, 38.05f)
                    close()
                }
            }
        }
        .build()
        return _regularExpression!!
    }

private var _regularExpression: ImageVector? = null
