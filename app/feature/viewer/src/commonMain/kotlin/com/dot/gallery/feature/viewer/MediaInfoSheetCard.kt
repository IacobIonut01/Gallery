package com.dot.gallery.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.util.LocalHazeState
import com.dot.gallery.core.presentation.util.hazeEffectScaled
import dev.chrisbanes.haze.blur.materials.HazeMaterials


/**
 * The shared card chrome used by every section of the media-details sheet —
 * blur-or-flat container, 16dp rounding, 600dp width cap. App-side slot content
 * (cloud backups, people cards…) uses the same wrapper to match.
 */
@Composable
fun MediaInfoSheetCard(
    modifier: Modifier = Modifier,
    innerPadding: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isBlurEnabled = LocalMediaViewerVisualPolicy.current.allowBlur
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceColorVariant = MaterialTheme.colorScheme.surfaceContainer
    val cardBackgroundModifier = remember(isBlurEnabled) {
        if (!isBlurEnabled) {
            Modifier.background(
                color = surfaceColorVariant,
                shape = RoundedCornerShape(16.dp)
            )
        } else {
            Modifier
        }
    }
    val cardHazeStyle = HazeMaterials.regular(
        containerColor = surfaceColor
    )
    Column(
        modifier = modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(cardBackgroundModifier)
            .hazeEffectScaled(
                state = LocalHazeState.current,
                style = cardHazeStyle
            )
            .then(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/**
 * Icon-well chrome used behind the small leading icons of info rows inside the
 * details sheet (shared so app slot rows match).
 */
@Composable
fun rememberMediaInfoIconBackgroundModifier(): Modifier {
    val isBlurEnabled = LocalMediaViewerVisualPolicy.current.allowBlur
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val iconBackgroundHazeStyle = HazeMaterials.thick(
        containerColor = surfaceContainerHigh
    )
    val iconBackgroundModifier = remember(isBlurEnabled) {
        if (!isBlurEnabled) {
            Modifier.background(
                color = surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp)
            )
        } else {
            Modifier
        }
    }
    return iconBackgroundModifier.hazeEffectScaled(
        state = LocalHazeState.current,
        style = iconBackgroundHazeStyle
    )
}
