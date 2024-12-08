package com.dot.gallery.feature_node.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.core.Settings.Misc.rememberExifDateFormat
import com.dot.gallery.feature_node.presentation.util.ExifMetadata
import com.dot.gallery.feature_node.presentation.util.getDate

@Stable
data class MediaDateCaption(
    val date: String,
    val deviceInfo: String? = null,
    val description: String
)

@Composable
fun rememberMediaDateCaption(
    exifMetadata: ExifMetadata?,
    media: Media
): MediaDateCaption {
    val deviceInfo = remember(exifMetadata) { exifMetadata?.lensDescription }
    val defaultDesc = stringResource(R.string.image_add_description)
    val description = remember(exifMetadata) { exifMetadata?.imageDescription ?: defaultDesc }
    val currentDateFormat by rememberExifDateFormat()
    return remember(media, currentDateFormat) {
        MediaDateCaption(
            date = media.definedTimestamp.getDate(currentDateFormat),
            deviceInfo = deviceInfo,
            description = description
        )
    }
}

@Composable
fun rememberMediaDateCaption(
    exifMetadata: ExifMetadata?,
    media: Media.UriMedia
): MediaDateCaption {
    val deviceInfo = remember(exifMetadata) { exifMetadata?.lensDescription }
    val defaultDesc = stringResource(R.string.image_add_description)
    val description = remember(exifMetadata) { exifMetadata?.imageDescription ?: defaultDesc }
    val currentDateFormat by rememberExifDateFormat()
    return remember(media, currentDateFormat) {
        MediaDateCaption(
            date = media.definedTimestamp.getDate(currentDateFormat),
            deviceInfo = deviceInfo,
            description = description
        )
    }
}