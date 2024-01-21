/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.util

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ShareCompat
import androidx.exifinterface.media.ExifInterface
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.mediaview.components.InfoRow
import com.dot.gallery.feature_node.presentation.mediaview.components.retrieveMetadata
import java.io.IOException

val sdcardRegex = "^/storage/[A-Z0-9]+-[A-Z0-9]+/.*$".toRegex()

fun Bitmap.flipHorizontally(): Bitmap {
    val matrix = Matrix().apply { postScale(-1f, 1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.flipVertically(): Bitmap {
    val matrix = Matrix().apply { postScale(1f, -1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun List<Media>.canBeTrashed(): Boolean {
    return find { it.path.matches(sdcardRegex) } == null
}

/**
 * first pair = trashable
 * second pair = non-trashable
 */
fun List<Media>.mediaPair(): Pair<List<Media>, List<Media>> {
    val trashableMedia = ArrayList<Media>()
    val nonTrashableMedia = ArrayList<Media>()
    forEach {
        if (it.path.matches(sdcardRegex)) {
            nonTrashableMedia.add(it)
        } else {
            trashableMedia.add(it)
        }
    }
    return trashableMedia to nonTrashableMedia
}

fun Media.canBeTrashed(): Boolean {
    return !path.matches(sdcardRegex)
}

@Composable
fun rememberActivityResult(onResultCanceled: () -> Unit = {}, onResultOk: () -> Unit = {}) =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {
            if (it.resultCode == RESULT_OK) onResultOk()
            if (it.resultCode == RESULT_CANCELED) onResultCanceled()
        }
    )


fun Media.writeRequest(
    contentResolver: ContentResolver,
) = IntentSenderRequest.Builder(MediaStore.createWriteRequest(contentResolver, arrayListOf(uri))).build()

fun List<Media>.writeRequest(
    contentResolver: ContentResolver,
) = IntentSenderRequest.Builder(MediaStore.createWriteRequest(contentResolver, map { it.uri })).build()

@Composable
fun rememberMediaInfo(media: Media, exifMetadata: ExifMetadata): List<InfoRow> {
    val context = LocalContext.current
    return remember(media) {
        media.retrieveMetadata(context, exifMetadata)
    }
}

@Composable
fun rememberExifMetadata(media: Media, exifInterface: ExifInterface): ExifMetadata {
    return remember(media) {
        ExifMetadata(exifInterface)
    }
}

@Composable
fun rememberExifInterface(media: Media, useDirectPath: Boolean = false): ExifInterface? {
    val context = LocalContext.current
    return remember(media) {
        if (useDirectPath) try { ExifInterface(media.path) } catch (_: IOException) { null }
        else getExifInterface(context, media.uri)
    }
}

@Throws(IOException::class)
fun getExifInterface(context: Context, uri: Uri): ExifInterface? {
    if (uri.isFromApps()) return null
    return try {
        ExifInterface(context.uriToPath(uri).toString())
    } catch (_: IOException) {
        null
    }
}

fun Context.uriToPath(uri: Uri?): String? {
    if (uri == null) return null
    val proj = arrayOf(MediaStore.MediaColumns.DATA)
    var path: String? = null
    val cursor: Cursor? = contentResolver.query(uri, proj, null, null, null)
    if (cursor != null && cursor.count != 0) {
        cursor.moveToFirst()
        path = try {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            cursor.getString(columnIndex)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
    cursor?.close()
    return path ?: FileUtils(this).getPath(uri)
}

fun Context.shareMedia(media: Media) {
    ShareCompat
        .IntentBuilder(this)
        .setType(media.mimeType)
        .addStream(media.uri)
        .startChooser()
}

fun Context.shareMedia(mediaList: List<Media>) {
    val mimeTypes =
        if (mediaList.find { it.duration != null } != null) {
            if (mediaList.find { it.duration == null } != null) "video/*,image/*" else "video/*"
        } else "image/*"

    val shareCompat = ShareCompat
        .IntentBuilder(this)
        .setType(mimeTypes)
    mediaList.forEach {
        shareCompat.addStream(it.uri)
    }
    shareCompat.startChooser()
}