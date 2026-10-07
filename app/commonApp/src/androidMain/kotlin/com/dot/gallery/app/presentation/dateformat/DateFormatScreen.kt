package com.dot.gallery.app.presentation.dateformat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.presentation.util.LocalHazeState
import com.dot.gallery.feature.settings.ui.DateFormatContent
import com.dot.gallery.core.presentation.util.systemDateTimePattern
import dev.chrisbanes.haze.hazeSource
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.image_sample_2

@Composable
fun DateFormatScreen() {
    val context = LocalContext.current
    val gridColumns by rememberGridSize()
    // Subscribe to configuration so locale / 24h changes recompose and re-resolve patterns.
    LocalConfiguration.current
    DateFormatContent(
        resolveSystemPattern = { field -> systemDateTimePattern(context, field) },
        gridColumns = gridColumns,
        previewImage = {
            Image(
                painter = painterResource(Res.drawable.image_sample_2),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(32.dp)
                    .hazeSource(LocalHazeState.current),
                contentScale = ContentScale.Crop
            )
        },
    )
}
