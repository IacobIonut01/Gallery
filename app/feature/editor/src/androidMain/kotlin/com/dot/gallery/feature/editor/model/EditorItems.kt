package com.dot.gallery.feature.editor.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Deblur
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette

import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import com.dot.gallery.domain.model.editor.DevelopCategory
import com.dot.gallery.feature.editor.adjustments.Markup

import com.dot.gallery.feature.editor.adjustments.varfilter.Tone
import com.dot.gallery.domain.util.isRaw
import com.dot.gallery.feature.editor.generated.resources.Res
import com.dot.gallery.feature.editor.generated.resources.editor_colour
import com.dot.gallery.feature.editor.generated.resources.editor_effects
import com.dot.gallery.feature.editor.generated.resources.editor_lighting
import com.dot.gallery.feature.editor.generated.resources.editor_more
import com.dot.gallery.feature.editor.generated.resources.editor_smart
import com.dot.gallery.feature.editor.generated.resources.filters
import com.dot.gallery.feature.editor.generated.resources.markup
import com.dot.gallery.feature.editor.generated.resources.raw_tab_colour
import com.dot.gallery.feature.editor.generated.resources.raw_tab_detail
import com.dot.gallery.feature.editor.generated.resources.raw_tab_output
import com.dot.gallery.feature.editor.generated.resources.raw_tab_tone
import com.dot.gallery.feature.editor.generated.resources.raw_tab_white_balance


@Keep
@Serializable
@Parcelize
enum class EditorItems : Parcelable {
    WhiteBalance,
    Tone,
    Detail,
    RawColour,
    Output,
    Smart,
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
            WhiteBalance -> stringResource(Res.string.raw_tab_white_balance)
            Tone -> stringResource(Res.string.raw_tab_tone)
            Detail -> stringResource(Res.string.raw_tab_detail)
            RawColour -> stringResource(Res.string.raw_tab_colour)
            Output -> stringResource(Res.string.raw_tab_output)
            Lighting -> stringResource(Res.string.editor_lighting)
            Filters -> stringResource(Res.string.filters)
            Markup -> stringResource(Res.string.markup)
            Colour -> stringResource(Res.string.editor_colour)
            Effects -> stringResource(Res.string.editor_effects)
            Smart -> stringResource(Res.string.editor_smart)
            More -> stringResource(Res.string.editor_more)
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
            Smart -> Icons.Outlined.AutoAwesome
            More -> Icons.Outlined.MoreHoriz
        }

    companion object {
        /**
         * Tabs visible for the current media. RAW images expose each develop category as its own tab
         * (White balance · Tone · Detail · Colour · Output) plus the non-overlapping tools, hiding
         * Lighting/Colour/Effects. Non-RAW images keep the full toolset (and never show develop tabs).
         */
        private val developItems = listOf(WhiteBalance, Tone, Detail, RawColour, Output)

        fun visibleItems(isRaw: Boolean, cutoutAvailable: Boolean = true): List<EditorItems> {
            val base = if (isRaw) listOf(Smart) + developItems + listOf(Filters, Markup, More)
            else entries.filterNot { it in developItems }
            return if (cutoutAvailable) base else base.filterNot { it == Smart }
        }
    }
}