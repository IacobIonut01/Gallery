/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs.scenes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dot.gallery.docs.showcase.generated.resources.Res
import com.dot.gallery.docs.showcase.generated.resources.docs_lake
import com.dot.gallery.domain.date.SystemDateFormatField
import com.dot.gallery.feature.settings.ui.DateFormatContent
import org.jetbrains.compose.resources.painterResource

/**
 * The real date-format settings screen. `resolveSystemPattern` is Android-only
 * in production (it reads the ICU pattern from the framework), so docs supply
 * fixed en-US skeletons — the same defaults a US-locale device would return.
 * The preview swatch uses the bundled lake photo like the app does with its
 * sample image.
 */
@Composable
fun DateFormatScene(state: String?) {
    DateFormatContent(
        resolveSystemPattern = { field -> docsSystemDatePattern(field) },
        gridColumns = 3,
        previewImage = {
            Image(
                painter = painterResource(Res.drawable.docs_lake),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(32.dp),
                contentScale = ContentScale.Crop,
            )
        },
    )
}

private fun docsSystemDatePattern(field: SystemDateFormatField): String = when (field) {
    SystemDateFormatField.WEEKLY -> "EEEE"
    SystemDateFormatField.DEFAULT -> "EEE, MMM d"
    SystemDateFormatField.EXTENDED -> "EEE, MMM d, yyyy"
    SystemDateFormatField.HEADER -> "MMM d\nHH:mm"
    SystemDateFormatField.EXTENDED_HEADER -> "MMM d, yyyy\nHH:mm"
    SystemDateFormatField.EXIF -> "MMM d, yyyy • HH:mm"
}
