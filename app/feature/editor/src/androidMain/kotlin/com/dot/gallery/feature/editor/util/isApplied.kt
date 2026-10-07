package com.dot.gallery.feature.editor.util

import androidx.annotation.Keep
import com.dot.gallery.feature.editor.model.Adjustment
import com.dot.gallery.feature.editor.adjustments.varfilter.VariableFilterTypes



@Keep
fun List<Adjustment>.isApplied(variableFilterTypes: VariableFilterTypes): Boolean {
    return any { it.name.equals(variableFilterTypes.name, ignoreCase = true) }
}