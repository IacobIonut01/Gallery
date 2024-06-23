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

public val Icons.Encrypted: ImageVector
    get() {
        if (_encrypted != null) {
            return _encrypted!!
        }
        _encrypted = Builder(name = "Encrypted",
                defaultWidth = 24.0.dp, defaultHeight = 24.0.dp, viewportWidth = 960.0f,
                viewportHeight = 960.0f).apply {
            path(fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveTo(420.0f, 600.0f)
                horizontalLineToRelative(120.0f)
                lineToRelative(-23.0f, -129.0f)
                quadToRelative(20.0f, -10.0f, 31.5f, -29.0f)
                reflectiveQuadToRelative(11.5f, -42.0f)
                quadToRelative(0.0f, -33.0f, -23.5f, -56.5f)
                reflectiveQuadTo(480.0f, 320.0f)
                quadToRelative(-33.0f, 0.0f, -56.5f, 23.5f)
                reflectiveQuadTo(400.0f, 400.0f)
                quadToRelative(0.0f, 23.0f, 11.5f, 42.0f)
                reflectiveQuadToRelative(31.5f, 29.0f)
                lineToRelative(-23.0f, 129.0f)
                close()
                moveTo(480.0f, 880.0f)
                quadToRelative(-139.0f, -35.0f, -229.5f, -159.5f)
                reflectiveQuadTo(160.0f, 444.0f)
                verticalLineToRelative(-244.0f)
                lineToRelative(320.0f, -120.0f)
                lineToRelative(320.0f, 120.0f)
                verticalLineToRelative(244.0f)
                quadToRelative(0.0f, 152.0f, -90.5f, 276.5f)
                reflectiveQuadTo(480.0f, 880.0f)
                close()
                moveTo(480.0f, 796.0f)
                quadToRelative(104.0f, -33.0f, 172.0f, -132.0f)
                reflectiveQuadToRelative(68.0f, -220.0f)
                verticalLineToRelative(-189.0f)
                lineToRelative(-240.0f, -90.0f)
                lineToRelative(-240.0f, 90.0f)
                verticalLineToRelative(189.0f)
                quadToRelative(0.0f, 121.0f, 68.0f, 220.0f)
                reflectiveQuadToRelative(172.0f, 132.0f)
                close()
                moveTo(480.0f, 480.0f)
                close()
            }
        }
        .build()
        return _encrypted!!
    }

private var _encrypted: ImageVector? = null
