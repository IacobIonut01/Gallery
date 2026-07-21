/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.rawdevelop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.edit.EditActivity
import com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons.MediaViewButton

/**
 * Media viewer bottom-bar action that opens a RAW in the editor, which starts on the self-contained
 * Develop tab. Only render this for RAW media when
 * [com.dot.gallery.core.decoder.NativeRawDecoder.isAvailable].
 */
@Composable
fun <T : Media> RawDevelopButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false,
) {
    val context = LocalContext.current
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.Tune,
        title = stringResource(R.string.raw_develop),
        enabled = enabled,
        followTheme = followTheme,
    ) {
        EditActivity.launchEditor(context, media.getUri())
    }
}
