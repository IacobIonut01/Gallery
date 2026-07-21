package com.dot.gallery.feature_node.domain.model.editor;

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Deblur
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Parcelize
enum class EditorItems : Parcelable {
    WhiteBalance,
    Tone,
    Detail,
    RawColour,
    Output,
    Lighting,
    Filters,
    Markup,
    Colour,
    Effects,
    More;

    /** For RAW develop tabs, the category this item drives; null for regular editor tools. */
    @IgnoredOnParcel
    val developCategory: DevelopCategory?
        get() = when (this) {
            WhiteBalance -> DevelopCategory.WhiteBalance
            Tone -> DevelopCategory.Tone
            Detail -> DevelopCategory.Detail
            RawColour -> DevelopCategory.Colour
            Output -> DevelopCategory.Output
            else -> null
        }

    @get:Composable
    val translatedName : String
        get() = when (this) {
            WhiteBalance -> stringResource(R.string.raw_tab_white_balance)
            Tone -> stringResource(R.string.raw_tab_tone)
            Detail -> stringResource(R.string.raw_tab_detail)
            RawColour -> stringResource(R.string.raw_tab_colour)
            Output -> stringResource(R.string.raw_tab_output)
            Lighting -> stringResource(R.string.editor_lighting)
            Filters -> stringResource(R.string.filters)
            Markup -> stringResource(R.string.markup)
            Colour -> stringResource(R.string.editor_colour)
            Effects -> stringResource(R.string.editor_effects)
            More -> stringResource(R.string.editor_more)
        }

    @IgnoredOnParcel
    val icon: ImageVector
        get() = when (this) {
            WhiteBalance -> Icons.Outlined.WbSunny
            Tone -> Icons.Outlined.Contrast
            Detail -> Icons.Outlined.Deblur
            RawColour -> Icons.Outlined.Palette
            Output -> Icons.Outlined.Image
            Lighting -> Icons.Outlined.WbSunny
            Filters -> Icons.Outlined.Filter
            Markup -> Icons.Outlined.Draw
            Colour -> Icons.Outlined.Palette
            Effects -> Icons.Outlined.AutoFixHigh
            More -> Icons.Outlined.MoreHoriz
        }

    companion object {
        /**
         * Tabs visible for the current media. RAW images expose each develop category as its own tab
         * (White balance · Tone · Detail · Colour · Output) plus the non-overlapping tools, hiding
         * Lighting/Colour/Effects. Non-RAW images keep the full toolset (and never show develop tabs).
         */
        private val developItems = listOf(WhiteBalance, Tone, Detail, RawColour, Output)

        fun visibleItems(isRaw: Boolean): List<EditorItems> =
            if (isRaw) developItems + listOf(Filters, Markup, More)
            else entries.filterNot { it in developItems }
    }
}