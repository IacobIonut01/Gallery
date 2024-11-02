package com.dot.gallery.feature_node.domain.model.editor

import com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter.VariableFilterTypes
import kotlinx.serialization.Serializable

@Serializable
sealed class EditorDestination {

    @Serializable
    data object Editor : EditorDestination()

    @Serializable
    data object Crop : EditorDestination()

    @Serializable
    data object Adjust : EditorDestination()

        @Serializable
        data class AdjustDetail(val adjustment: VariableFilterTypes) : EditorDestination()

    @Serializable
    data object Filters : EditorDestination()

    @Serializable
    data object Markup : EditorDestination()

        @Serializable
        data object MarkupDraw : EditorDestination()

            @Serializable
            data object MarkupDrawSize : EditorDestination()

            @Serializable
            data object MarkupDrawColor : EditorDestination()

        @Serializable
        data object MarkupErase : EditorDestination()

            @Serializable
            data object MarkupEraseSize : EditorDestination()

        @Serializable
        data object ExternalEditor : EditorDestination()

}