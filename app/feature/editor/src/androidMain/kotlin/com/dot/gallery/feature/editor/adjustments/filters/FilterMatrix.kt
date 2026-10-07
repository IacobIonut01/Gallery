package com.dot.gallery.feature.editor.adjustments.filters

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature.editor.util.applyColorMatrix

internal fun applyViaColorMatrix(bitmap: Bitmap, cm: ColorMatrix): Bitmap =
    applyColorMatrix(bitmap, cm.values)
