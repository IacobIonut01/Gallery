package com.dot.gallery.feature_node.domain.model.editor

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CropDin
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Texture
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class EffectTool {
    Posterize,
    Edges,
    Borders;

    @get:Composable
    val translatedName: String
        get() = when (this) {
            Posterize -> stringResource(R.string.effect_posterize)
            Edges -> stringResource(R.string.effect_edges)
            Borders -> stringResource(R.string.effect_borders)
        }

    val icon: ImageVector
        get() = when (this) {
            Posterize -> Icons.Outlined.Texture
            Edges -> Icons.Outlined.GridOn
            Borders -> Icons.Outlined.CropDin
        }
}
