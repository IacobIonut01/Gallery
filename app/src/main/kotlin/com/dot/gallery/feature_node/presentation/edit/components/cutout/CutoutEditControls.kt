package com.dot.gallery.feature_node.presentation.edit.components.cutout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.edit.components.adjustment.SelectableItem
import com.dot.gallery.feature_node.presentation.edit.components.core.SupportiveLazyLayout
import com.dot.gallery.feature_node.presentation.mediaview.components.media.CutoutState
import com.dot.gallery.feature_node.presentation.mediaview.components.media.ZoomablePagerImagePointTool
import com.dot.gallery.feature_node.presentation.mediaview.rememberedDerivedState

private data class CutoutControl(
    val key: String,
    val icon: ImageVector,
    val selected: Boolean,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

/**
 * Bottom controls for the interactive cut-out mode: Include / Exclude prompt tools and Reset. Copy /
 * Share live in the header (replacing the crop toolbar) and Undo / Redo use the editor's top bar, so
 * this panel intentionally stays minimal.
 */
@Composable
fun CutoutEditControls(
    cutoutState: CutoutState,
    isSupportingPanel: Boolean,
    onToolChange: (ZoomablePagerImagePointTool) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTool = cutoutState.activeTool

    val controls by rememberedDerivedState(activeTool) {
        listOf(
            CutoutControl(
                key = "include",
                icon = Icons.Outlined.Add,
                selected = activeTool == ZoomablePagerImagePointTool.ADD,
                enabled = true,
            ) { onToolChange(ZoomablePagerImagePointTool.ADD) },
            CutoutControl(
                key = "exclude",
                icon = Icons.Outlined.Remove,
                selected = activeTool == ZoomablePagerImagePointTool.REMOVE,
                enabled = true,
            ) { onToolChange(ZoomablePagerImagePointTool.REMOVE) },
        )
    }

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
        items(items = controls, key = { it.key }) { control ->
            SelectableItem(
                icon = control.icon,
                title = titleFor(control.key),
                selected = control.selected,
                enabled = control.enabled,
                horizontal = isSupportingPanel,
                onItemClick = control.onClick
            )
        }
    }
}

@Composable
private fun titleFor(key: String): String = when (key) {
    "include" -> stringResource(R.string.cutout_include)
    "exclude" -> stringResource(R.string.cutout_exclude)
    "reset" -> stringResource(R.string.cutout_reset)
    else -> ""
}
