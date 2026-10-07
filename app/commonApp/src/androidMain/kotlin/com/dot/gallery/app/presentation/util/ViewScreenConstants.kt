package com.dot.gallery.app.presentation.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.core.SheetDetent
import com.dot.gallery.core.logging.printWarning



object ViewScreenConstants {
    val BOTTOM_BAR_HEIGHT = 64.dp

    fun ImageOnly(height: () -> Dp = { BOTTOM_BAR_HEIGHT }) =
        SheetDetent("imageOnly") { _, _ ->
            height()
        }

    @Suppress("FunctionName")
    fun FullyExpanded(setHeight: (Dp) -> Unit) =
        SheetDetent("fully-expanded") { containerHeight, sheetHeight ->
            printWarning("Sheet height: $sheetHeight, container height: $containerHeight")
            setHeight(sheetHeight)
            sheetHeight
        }
}
