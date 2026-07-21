package com.dot.gallery.feature_node.domain.model.editor

import com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter.VariableFilterTypes
import kotlinx.serialization.Serializable

@Serializable
sealed class EditorDestination {

    @Serializable
    data object Editor : EditorDestination()

    @Serializable
    data class Develop(val category: DevelopCategory) : EditorDestination()

    @Serializable
    data object Markup : EditorDestination()

    @Serializable
    data object MarkupDraw : EditorDestination()

    @Serializable
    data object Filters : EditorDestination()

    @Serializable
    data object Lighting : EditorDestination()

    @Serializable
    data object Colour : EditorDestination()

    @Serializable
    data object Effects : EditorDestination()

    @Serializable
    data object More : EditorDestination()

    @Serializable
    data class AdjustDetail(val adjustment: VariableFilterTypes) : EditorDestination()

}

/**
 * The top-level destination a tab item navigates to. RAW develop items resolve to a
 * [EditorDestination.Develop] carrying their [DevelopCategory]; regular tools map to their own
 * destinations.
 */
fun EditorItems.toEditorDestination(): EditorDestination =
    developCategory?.let { EditorDestination.Develop(it) } ?: when (this) {
        EditorItems.Lighting -> EditorDestination.Lighting
        EditorItems.Filters -> EditorDestination.Filters
        EditorItems.Markup -> EditorDestination.Markup
        EditorItems.Colour -> EditorDestination.Colour
        EditorItems.Effects -> EditorDestination.Effects
        EditorItems.More -> EditorDestination.More
        else -> EditorDestination.Editor
    }