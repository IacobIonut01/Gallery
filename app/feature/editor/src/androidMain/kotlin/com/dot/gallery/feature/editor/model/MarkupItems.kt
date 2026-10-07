package com.dot.gallery.feature.editor.model

import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.ui.core.icons.InkHighlighter
import com.dot.gallery.ui.core.icons.InkMarker
import com.dot.gallery.ui.core.icons.Ink_Eraser
import com.dot.gallery.ui.core.icons.Stylus
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

import com.dot.gallery.ui.core.Icons as DotIcons
import com.dot.gallery.feature.editor.generated.resources.Res
import com.dot.gallery.feature.editor.generated.resources.type_blur
import com.dot.gallery.feature.editor.generated.resources.type_erase
import com.dot.gallery.feature.editor.generated.resources.type_highlighter
import com.dot.gallery.feature.editor.generated.resources.type_marker
import com.dot.gallery.feature.editor.generated.resources.type_mosaic
import com.dot.gallery.feature.editor.generated.resources.type_pan
import com.dot.gallery.feature.editor.generated.resources.type_stylus
import com.dot.gallery.feature.editor.generated.resources.type_text

@Serializable
@Parcelize
enum class MarkupItems : Parcelable {
    Stylus,
    Highlighter,
    Marker,
    Blur,
    Mosaic,
    Text,
    Eraser,
    Pan;

    @get:Composable
    val translatedName get() = when (this) {
        Stylus -> stringResource(Res.string.type_stylus)
        Highlighter -> stringResource(Res.string.type_highlighter)
        Marker -> stringResource(Res.string.type_marker)
        Blur -> stringResource(Res.string.type_blur)
        Mosaic -> stringResource(Res.string.type_mosaic)
        Text -> stringResource(Res.string.type_text)
        Eraser -> stringResource(Res.string.type_erase)
        Pan -> stringResource(Res.string.type_pan)
    }

    @IgnoredOnParcel
    val icon: ImageVector
        get() = when (this) {
            Stylus -> DotIcons.Stylus
            Highlighter -> DotIcons.InkHighlighter
            Marker -> DotIcons.InkMarker
            Blur -> Icons.Outlined.BlurOn
            Mosaic -> Icons.Outlined.GridOn
            Text -> Icons.Outlined.TextFields
            Eraser -> DotIcons.Ink_Eraser
            Pan -> Icons.Outlined.PanTool
        }
}