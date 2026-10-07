/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.PersistableBundle
import android.widget.Toast
import com.dot.gallery.core.util.SdkCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.core.net.toFile
import com.dot.gallery.feature.viewer.InfoRow
import com.dot.gallery.domain.model.Media
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.domain.model.Vault
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.app.presentation.mediaview.components.retrieveMetadata
import com.dot.gallery.feature.viewer.components.video.VideoHdrInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.dot.gallery.cloud.util.CloudMediaDownloader
import java.io.File
import java.io.FileOutputStream
import com.dot.gallery.core.util.or
import com.dot.gallery.data.data_source.KeychainHolder
import com.dot.gallery.core.Settings.Misc.rememberExifDateFormat
import com.dot.gallery.domain.util.isCloud
import com.dot.gallery.domain.util.isEncrypted
import com.dot.gallery.domain.uri.path
import com.dot.gallery.domain.uri.scheme
import com.dot.gallery.core.logging.printError
import com.dot.gallery.core.logging.printWarning
import com.dot.gallery.BuildConfig



@Composable
fun rememberBitmapPainter(bitmap: Bitmap): State<Painter> {
    return remember(bitmap) { derivedStateOf { BitmapPainter(image = bitmap.asImageBitmap()) } }
}

fun FloatArray.to3x3Matrix(): FloatArray {
    return floatArrayOf(
        this[0], this[1], this[2],
        this[5], this[6], this[7],
        this[10], this[11], this[12]
    )
}


@Composable
fun <T : Media> rememberMediaInfo(
    media: T,
    exifMetadata: MediaMetadata?,
    videoHdr: VideoHdrInfo = VideoHdrInfo.NONE,
    onLabelClick: () -> Unit
): List<InfoRow> {
    val context = LocalContext.current
    val exifDateFormat by rememberExifDateFormat()
    return remember(exifMetadata, media, exifDateFormat, videoHdr) {
        media.retrieveMetadata(context, exifDateFormat, exifMetadata, onLabelClick, videoHdr)
    }
}
