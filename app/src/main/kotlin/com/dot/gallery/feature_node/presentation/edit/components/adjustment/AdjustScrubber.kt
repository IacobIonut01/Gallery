package com.dot.gallery.feature_node.presentation.edit.components.adjustment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dot.gallery.feature_node.domain.model.editor.Adjustment
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter
import com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter.VariableFilterTypes
import com.dot.gallery.feature_node.presentation.edit.components.core.HorizontalScrubber
import com.dot.gallery.feature_node.presentation.edit.components.core.VerticalScrubber
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun shouldDispatchAdjustmentPreview(currentValue: Float, newValue: Float): Boolean =
    abs(currentValue - newValue) > 1e-4f

@Composable
fun AdjustScrubber(
    adjustment: VariableFilterTypes,
    appliedAdjustments: List<Adjustment> = emptyList(),
    modifier: Modifier = Modifier,
    displayValue: (Float) -> String = { (it * 100f).roundToInt().toString() },
    onAdjustmentPreview: (Adjustment) -> Unit = {},
    onAdjustmentChange: (Adjustment) -> Unit = {},
    isSupportingPanel: Boolean,
) {
    val defaultAdjustment = remember(adjustment, appliedAdjustments) {
        (appliedAdjustments.findLast { it.name.equals(adjustment.name, ignoreCase = true) } as VariableFilter?)
            ?: adjustment.createDefaultFilter()
    }
    var currentAdjustment by remember(defaultAdjustment) {
        mutableStateOf(defaultAdjustment)
    }
    var currentValue by rememberSaveable(currentAdjustment, appliedAdjustments) {
        mutableFloatStateOf(currentAdjustment.value)
    }
    DisposableEffect(Unit) {
        onDispose {
            val applied = appliedAdjustments.findLast {
                it.name.equals(adjustment.name, ignoreCase = true)
            } as VariableFilter?
            // Commit when the value differs from what's currently applied. If an adjustment is
            // already applied, scrubbing back to the default must still be committed so the
            // effect is removed; previously this was skipped, leaving the old value (e.g. 100)
            // in place. Only skip when nothing was applied and the value is still the default.
            if (currentAdjustment != applied
                && (applied != null || currentAdjustment != adjustment.createDefaultFilter())
            ) {
                onAdjustmentChange(currentAdjustment)
            }
        }
    }
    if (isSupportingPanel) {
        Box(modifier = modifier.fillMaxWidth(0.5f), contentAlignment = Alignment.Center) {
            VerticalScrubber(
                minValue = defaultAdjustment.minValue,
                maxValue = defaultAdjustment.maxValue,
                defaultValue = defaultAdjustment.defaultValue,
                allowNegative = defaultAdjustment.minValue < 0f,
                currentValue = currentValue,
                displayValue = displayValue,
                onValueChanged = { _, newValue ->
                    if (shouldDispatchAdjustmentPreview(currentValue, newValue)) {
                        currentValue = newValue
                        currentAdjustment = adjustment.createFilter(newValue)
                        onAdjustmentPreview(currentAdjustment)
                    }
                }
            )
        }
    } else {
        HorizontalScrubber(
            modifier = modifier,
            minValue = defaultAdjustment.minValue,
            maxValue = defaultAdjustment.maxValue,
            defaultValue = defaultAdjustment.defaultValue,
            allowNegative = defaultAdjustment.minValue < 0f,
            currentValue = currentValue,
            displayValue = displayValue,
            onValueChanged = { _, newValue ->
                if (shouldDispatchAdjustmentPreview(currentValue, newValue)) {
                    currentValue = newValue
                    currentAdjustment = adjustment.createFilter(newValue)
                    onAdjustmentPreview(currentAdjustment)
                }
            }
        )
    }
}