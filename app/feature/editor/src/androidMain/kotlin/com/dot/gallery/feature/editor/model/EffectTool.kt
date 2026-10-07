package com.dot.gallery.feature.editor.model

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CropDin
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Texture
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import kotlinx.serialization.Serializable
import com.dot.gallery.feature.editor.adjustments.varfilter.Borders
import com.dot.gallery.feature.editor.adjustments.varfilter.Edges
import com.dot.gallery.feature.editor.generated.resources.Res
import com.dot.gallery.feature.editor.generated.resources.effect_borders
import com.dot.gallery.feature.editor.generated.resources.effect_edges
import com.dot.gallery.feature.editor.generated.resources.effect_posterize


@Keep
@Serializable
enum class EffectTool {
    Posterize,
    Edges,
    Borders;

    @get:Composable
    val translatedName: String
        get() = when (this) {
            Posterize -> stringResource(Res.string.effect_posterize)
            Edges -> stringResource(Res.string.effect_edges)
            Borders -> stringResource(Res.string.effect_borders)
        }

    val icon: ImageVector
        get() = when (this) {
            Posterize -> Icons.Outlined.Texture
            Edges -> Icons.Outlined.GridOn
            Borders -> Icons.Outlined.CropDin
        }
}
