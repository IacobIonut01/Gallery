package com.dot.gallery.feature_node.presentation.edit.components.cutout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.edit.components.adjustment.SelectableItem
import com.dot.gallery.feature_node.presentation.edit.components.core.SupportiveLazyLayout

private enum class SmartTool { CUTOUT, BACKGROUND_REMOVAL }

/**
 * Entry selector for the Smart category. Exposes the two subject tools — Cutout and Background
 * Removal — which share the same interactive workings; [onToolClick] reports whether the background
 * removal variant was chosen.
 */
@Composable
fun SmartSelector(
    isSupportingPanel: Boolean,
    onToolClick: (backgroundRemoval: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tools = listOf(SmartTool.CUTOUT, SmartTool.BACKGROUND_REMOVAL)
    val padding = if (isSupportingPanel) PaddingValues(0.dp) else PaddingValues(horizontal = 12.dp)

    SupportiveLazyLayout(
        modifier = modifier
            .animateContentSize()
            .fillMaxWidth()
            .then(
                if (isSupportingPanel) Modifier.clipToBounds().clip(RoundedCornerShape(16.dp))
                else Modifier
            ),
        contentPadding = padding,
        isSupportingPanel = isSupportingPanel
    ) {
        items(items = tools, key = { it.name }) { tool ->
            when (tool) {
                SmartTool.CUTOUT -> SelectableItem(
                    icon = Icons.Outlined.ContentCut,
                    title = stringResource(R.string.cutout_tool),
                    selected = false,
                    horizontal = isSupportingPanel,
                    onItemClick = { onToolClick(false) }
                )
                SmartTool.BACKGROUND_REMOVAL -> SelectableItem(
                    icon = Icons.Outlined.Layers,
                    title = stringResource(R.string.cutout_background_removal),
                    selected = false,
                    horizontal = isSupportingPanel,
                    onItemClick = { onToolClick(true) }
                )
            }
        }
    }
}
