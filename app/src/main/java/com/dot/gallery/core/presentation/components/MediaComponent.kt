package com.dot.gallery.core.presentation.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dot.gallery.core.presentation.components.util.advancedShadow
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.ui.theme.Dimens
import com.dot.gallery.ui.theme.Shapes
import java.io.File

@Composable
fun MediaComponent(
    media: Media,
    preloadRequestBuilder: RequestBuilder<Drawable>,
    onItemClick: (Media) -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .border(
                width = .2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = Shapes.small
            )
            .clip(Shapes.small)
            .background(
                color = MaterialTheme.colorScheme.surface,
            )
            .clickable {
                onItemClick(media)
            },
    ) {
        MediaImage(media = media, preloadRequestBuilder)
        if (media.duration != null) {
            VideoDurationHeader(media = media)
        }
    }
}

@Composable
fun BoxScope.VideoDurationHeader(media: Media) {
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(all = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .advancedShadow(
                    cornersRadius = 2.dp,
                    shadowBlurRadius = 6.dp,
                    alpha = 0.1f,
                    offsetY = (-2).dp
                ),
            text = media.formatTime(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.size(2.dp))
        Image(
            modifier = Modifier
                .size(16.dp)
                .advancedShadow(
                    cornersRadius = 2.dp,
                    shadowBlurRadius = 6.dp,
                    alpha = 0.1f,
                    offsetY = (-2).dp
                ),
            imageVector = Icons.Rounded.PlayCircle,
            colorFilter = ColorFilter.tint(color = Color.White),
            contentDescription = "Video"
        )
    }
}

/**
 * @param model -> Data source to display the image
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaImage(media: Media, preloadRequestBuilder: RequestBuilder<Drawable>) {
    GlideImage(
        modifier = Modifier
            .aspectRatio(1f)
            .size(Dimens.Photo()),
        model = File(media.path),
        contentDescription = media.label,
        contentScale = ContentScale.Crop,
    ) {
        it.thumbnail(preloadRequestBuilder)
    }
}
