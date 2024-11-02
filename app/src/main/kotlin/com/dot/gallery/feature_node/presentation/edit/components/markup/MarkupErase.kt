package com.dot.gallery.feature_node.presentation.edit.components.markup

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.dot.gallery.feature_node.domain.model.editor.EditorDestination
import com.dot.gallery.feature_node.domain.model.editor.MarkupEraseItems
import com.dot.gallery.feature_node.domain.model.editor.PathProperties
import com.dot.gallery.feature_node.presentation.edit.components.adjustment.SelectableItem
import com.dot.gallery.feature_node.presentation.edit.components.core.SupportiveLazyLayout

@Composable
fun MarkupEraseSelector(
    paths: List<Pair<Path, PathProperties>>,
    pathsUndone: List<Pair<Path, PathProperties>>,
    undoLastPath: () -> Unit,
    redoLastPath: () -> Unit,
    navigate: (EditorDestination) -> Unit,
    isSupportingPanel: Boolean,
) {

    val padding = remember(isSupportingPanel) {
        if (isSupportingPanel) PaddingValues(0.dp) else PaddingValues(16.dp)
    }

    SupportiveLazyLayout(
        modifier = Modifier.fillMaxWidth()
            .then(
                if (isSupportingPanel) Modifier
                    .padding(top = 16.dp)
                    .clipToBounds()
                    .clip(RoundedCornerShape(16.dp))
                else Modifier
            ),
        isSupportingPanel = isSupportingPanel,
        contentPadding = padding
    ) {
        itemsIndexed(
            items = MarkupEraseItems.entries,
            key = { _, it -> it.name }
        ) { index, item ->
            val isEnabled by remember(item, paths, pathsUndone) {
                derivedStateOf {
                    when (item) {
                        MarkupEraseItems.Undo -> paths.isNotEmpty()
                        MarkupEraseItems.Redo -> pathsUndone.isNotEmpty()
                        else -> true
                    }
                }
            }
            SelectableItem(
                icon = item.icon,
                title = item.name,
                enabled = isEnabled,
                horizontal = isSupportingPanel,
                onItemClick = {
                    when (item) {
                        MarkupEraseItems.Size -> navigate(EditorDestination.MarkupEraseSize)
                        MarkupEraseItems.Undo -> undoLastPath()
                        MarkupEraseItems.Redo -> redoLastPath()
                    }
                }
            )
            if (isSupportingPanel && index < MarkupEraseItems.entries.size - 1) {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}
