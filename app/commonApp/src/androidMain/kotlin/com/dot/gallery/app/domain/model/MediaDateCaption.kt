/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.app.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaDateCaption
import com.dot.gallery.core.Settings.Misc.rememberExifDateFormat
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.data.entity.lensDescription
import com.dot.gallery.domain.model.definedTimestamp
import com.dot.gallery.domain.date.getDate
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.image_add_description


@Composable
fun rememberMediaDateCaption(
    exifMetadata: MediaMetadata?,
    media: Media
): MediaDateCaption {
    val deviceInfo = remember(exifMetadata, media) { exifMetadata?.lensDescription }
    val defaultDesc = stringResource(Res.string.image_add_description)
    val description = remember(exifMetadata, media) { exifMetadata?.imageDescription ?: defaultDesc }
    val currentDateFormat by rememberExifDateFormat()
    return remember(media, exifMetadata, currentDateFormat) {
        MediaDateCaption(
            date = media.definedTimestamp.getDate(currentDateFormat),
            deviceInfo = deviceInfo,
            description = description
        )
    }
}
