package com.dot.gallery.ui.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.dot.gallery.ui.core.Icons

public val Icons.Ink_Eraser: ImageVector
	get() {
		if (_Ink_Eraser != null) {
			return _Ink_Eraser!!
		}
		_Ink_Eraser = ImageVector.Builder(
            name = "Ink_eraser",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
			path(
    			fill = SolidColor(Color.Black),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(690f, 720f)
				horizontalLineToRelative(190f)
				verticalLineToRelative(80f)
				horizontalLineTo(610f)
				close()
				moveToRelative(-500f, 80f)
				lineToRelative(-85f, -85f)
				quadToRelative(-23f, -23f, -23.5f, -57f)
				reflectiveQuadToRelative(22.5f, -58f)
				lineToRelative(440f, -456f)
				quadToRelative(23f, -24f, 56.5f, -24f)
				reflectiveQuadToRelative(56.5f, 23f)
				lineToRelative(199f, 199f)
				quadToRelative(23f, 23f, 23f, 57f)
				reflectiveQuadToRelative(-23f, 57f)
				lineTo(520f, 800f)
				close()
				moveToRelative(296f, -80f)
				lineToRelative(314f, -322f)
				lineToRelative(-198f, -198f)
				lineToRelative(-442f, 456f)
				lineToRelative(64f, 64f)
				close()
				moveToRelative(-6f, -240f)
			}
		}.build()
		return _Ink_Eraser!!
	}

private var _Ink_Eraser: ImageVector? = null
