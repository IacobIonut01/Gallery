package com.dot.gallery.core.presentation.components

import android.media.ThumbnailUtils
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import coil.compose.AsyncImage
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.ui.theme.Dimens
import com.dot.gallery.ui.theme.Shapes
import java.io.File

@Composable
fun MediaComponent(
    media: Media,
    onItemClick: (Media) -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(Shapes.small)
            .background(
                color = MaterialTheme.colorScheme.surface,
            )
            .clickable {
                onItemClick(media)
            },
    ) {
        if (media.duration == null) {
            Image(media = media, model = File(media.path))
        } else {
            Image(
                media = media,
                model = ThumbnailUtils.createVideoThumbnail(
                    File(media.path),
                    Size(200, 200),
                    null
                )
            )
        }
    }
}

/**
 * @param model -> Data source to display the image
 */
@Composable
private fun Image(media: Media, model: Any?) {
    AsyncImage(
        modifier = Modifier
            .aspectRatio(1f)
            .size(Dimens.Photo()),
        model = model,
        contentDescription = media.label,
        contentScale = ContentScale.Crop,
        onError = {
            it.result.throwable.printStackTrace()
        }
    )
}