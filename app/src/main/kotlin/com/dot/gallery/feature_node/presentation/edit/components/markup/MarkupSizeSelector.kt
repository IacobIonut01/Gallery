package com.dot.gallery.feature_node.presentation.edit.components.markup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dot.gallery.feature_node.domain.model.editor.PathProperties
import com.dot.gallery.feature_node.presentation.edit.components.core.HorizontalScrubber
import com.dot.gallery.feature_node.presentation.edit.components.core.SupportiveLayout
import com.dot.gallery.feature_node.presentation.edit.components.core.VerticalScrubber
import kotlin.math.roundToInt

@Composable
fun MarkupSizeSelector(
    modifier: Modifier = Modifier,
    currentPathProperty: PathProperties,
    setCurrentPathProperty: (PathProperties) -> Unit,
    isSupportingPanel: Boolean,
) {
    var currentValue by rememberSaveable {
        mutableFloatStateOf(currentPathProperty.strokeWidth)
    }
    var previewPathProperty by remember {
        mutableStateOf(currentPathProperty)
    }

    SupportiveLayout(
        modifier = modifier.fillMaxWidth(),
        isSupportingPanel = isSupportingPanel,
    ) {
        MarkupPathPreview(
            pathProperties = previewPathProperty,
            isSupportingPanel = isSupportingPanel
        )

        if (isSupportingPanel) {
            Box(modifier = modifier.fillMaxWidth(0.5f), contentAlignment = Alignment.CenterEnd) {
                VerticalScrubber(
                    modifier = Modifier,
                    defaultValue = 20f,
                    minValue = 1f,
                    maxValue = 100f,
                    currentValue = currentValue,
                    allowNegative = false,
                    displayValue = { "${it.roundToInt()} px" },
                    onValueChanged = { isScrolling, newValue ->
                        currentValue = newValue
                        previewPathProperty = currentPathProperty.copy(strokeWidth = newValue)
                        if (!isScrolling) {
                            setCurrentPathProperty(currentPathProperty.copy(strokeWidth = newValue))
                        }
                    }
                )
            }
        } else {
            HorizontalScrubber(
                defaultValue = 20f,
                minValue = 1f,
                maxValue = 100f,
                currentValue = currentValue,
                allowNegative = false,
                displayValue = { "${it.roundToInt()} px" },
                onValueChanged = { isScrolling, newValue ->
                    currentValue = newValue
                    previewPathProperty = previewPathProperty.copy(strokeWidth = newValue)
                    if (!isScrolling) {
                        setCurrentPathProperty(currentPathProperty.copy(strokeWidth = newValue))
                    }
                }
            )
        }
    }
}