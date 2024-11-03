package com.dot.gallery.feature_node.presentation.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.core.SheetDetent

object ViewScreenConstants {
    val BOTTOM_BAR_HEIGHT = 104.dp
    val BOTTOM_BAR_HEIGHT_SLIM = 100.dp

    val ImageOnly = SheetDetent("imageOnly") { _, _ -> BOTTOM_BAR_HEIGHT }

    @Suppress("FunctionName")
    fun FullyExpanded(setHeight: (Dp) -> Unit) =
        SheetDetent("fully-expanded") { _, sheetHeight ->
            setHeight(sheetHeight)
            sheetHeight
        }
}