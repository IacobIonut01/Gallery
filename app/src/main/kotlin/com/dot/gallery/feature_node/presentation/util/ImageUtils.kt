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
import com.dot.gallery.feature_node.domain.model.ImageFilter
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.mediaview.components.InfoRow
import com.dot.gallery.feature_node.presentation.mediaview.components.retrieveMetadata
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter
import java.io.IOException

val sdcardRegex = "^/storage/[A-Z0-9]+-[A-Z0-9]+/.*$".toRegex()


fun Context.gpuImage(bitmap: Bitmap) = GPUImage(this).apply { setImage(bitmap) }

fun GPUImage.mapToImageFilters(): List<ImageFilter> {
    val gpuImage = this
    val imgFilters: ArrayList<ImageFilter> = ArrayList()

    //region:: Filters
    // Normal
    GPUImageFilter().also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "None",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Retro
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.2f, 0.0f,
            0.1f, 0.1f, 1.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Retro",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Just
    GPUImageColorMatrixFilter(
        0.9f,
        floatArrayOf(
            0.4f, 0.6f, 0.5f, 0.0f,
            0.0f, 0.4f, 1.0f, 0.0f,
            0.05f, 0.1f, 0.4f, 0.4f,
            1.0f, 1.0f, 1.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Just",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Hume
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.25f, 0.0f, 0.2f, 0.0f,
            0.0f, 1.0f, 0.2f, 0.0f,
            0.0f, 0.3f, 1.0f, 0.3f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Hume",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Desert
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.6f, 0.4f, 0.2f, 0.05f,
            0.0f, 0.8f, 0.3f, 0.05f,
            0.3f, 0.3f, 0.5f, 0.08f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Desert",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Old Times
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, 0.05f, 0.0f, 0.0f,
            -0.2f, 1.1f, -0.2f, 0.11f,
            0.2f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Old Times",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Limo
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, 0.0f, 0.08f, 0.0f,
            0.4f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.1f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Limo",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Sepia
    GPUImageSepiaToneFilter().also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Sepia",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Solar
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.5f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Solar",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Wole
    GPUImageSaturationFilter(2.0f).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Wole",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Neutron
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0f, 1f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0.6f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Neutron",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Bright
    GPUImageRGBFilter(1.1f, 1.3f, 1.6f).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Bright",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Milk
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.64f, 0.5f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Milk",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // BW
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "BW",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Clue
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Clue",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Muli
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Muli",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Aero
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0f, 0f, 1f, 0f,
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Aero",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Classic
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.763f, 0.0f, 0.2062f, 0f,
            0.0f, 0.9416f, 0.0f, 0f,
            0.1623f, 0.2614f, 0.8052f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Classic",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Atom
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.5162f, 0.3799f, 0.3247f, 0f,
            0.039f, 1.0f, 0f, 0f,
            -0.4773f, 0.461f, 1.0f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Atom",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Mars
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            0.0f, 0.0f, 0.5183f, 0.3183f,
            0.0f, 0.5497f, 0.5416f, 0f,
            0.5237f, 0.5269f, 0.0f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Mars",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }

    // Yeli
    GPUImageColorMatrixFilter(
        1.0f,
        floatArrayOf(
            1.0f, -0.3831f, 0.3883f, 0.0f,
            0.0f, 1.0f, 0.2f, 0f,
            -0.1961f, 0.0f, 1.0f, 0f,
            0f, 0f, 0f, 1f
        )
    ).also { filter ->
        gpuImage.setFilter(filter)
        imgFilters.add(
            ImageFilter(
                name = "Yeli",
                filter = filter,
                filterPreview = gpuImage.bitmapWithFilterApplied
            )
        )
    }
    //endregion
    return imgFilters
}

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
fun rememberMediaInfo(media: Media, exifMetadata: ExifMetadata, onLabelClick: () -> Unit): List<InfoRow> {
    val context = LocalContext.current
    return remember(media) {
        media.retrieveMetadata(context, exifMetadata, onLabelClick)
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