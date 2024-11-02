package com.dot.gallery.feature_node.domain.model.editor;

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Parcelize
enum class EditorItems : Parcelable {
    Crop,
    Adjust,
    Filters,
    Markup;

    @IgnoredOnParcel
    val icon: ImageVector
        get() = when (this) {
            Crop -> Icons.Outlined.Crop
            Adjust -> Icons.Outlined.Adjust
            Filters -> Icons.Outlined.Filter
            Markup -> Icons.Outlined.Draw
        }
}