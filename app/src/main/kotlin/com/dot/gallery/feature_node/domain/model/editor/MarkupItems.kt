package com.dot.gallery.feature_node.domain.model.editor

import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.ui.graphics.vector.ImageVector
import com.dot.gallery.ui.core.icons.Ink_Eraser
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import com.dot.gallery.ui.core.Icons as DotIcons

@Serializable
@Parcelize
enum class MarkupEraseItems : Parcelable {
    Undo,
    Size,
    Redo;

    @IgnoredOnParcel
    val icon: ImageVector
        get() = when (this) {
            Size -> Icons.Outlined.Brush
            Undo -> Icons.AutoMirrored.Outlined.Undo
            Redo -> Icons.AutoMirrored.Outlined.Redo
        }
}


@Serializable
@Parcelize
enum class MarkupDrawItems : Parcelable {
    Undo,
    Size,
    Color,
    Redo;

    @IgnoredOnParcel
    val icon: ImageVector
        get() = when (this) {
            Size -> Icons.Outlined.Brush
            Color -> Icons.Outlined.ColorLens
            Undo -> Icons.AutoMirrored.Outlined.Undo
            Redo -> Icons.AutoMirrored.Outlined.Redo
        }
}

@Serializable
@Parcelize
enum class MarkupItems : Parcelable {
    Undo,
    Draw,
    Erase,
    Redo;

    @IgnoredOnParcel
    val icon: ImageVector
        get() = when (this) {
            Draw -> Icons.Outlined.Draw
            Erase -> DotIcons.Ink_Eraser
            Undo -> Icons.AutoMirrored.Outlined.Undo
            Redo -> Icons.AutoMirrored.Outlined.Redo
        }
}