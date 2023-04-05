package com.dot.gallery.feature_node.presentation.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.data.data_types.getMediaUri
import com.dot.gallery.feature_node.domain.model.Media
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws

@Throws(IOException::class)
fun Uri.getExifInterface(): ExifInterface {
    return ExifInterface(toFile())
}

fun Context.uriToPath(uri: Uri?): String? {
    if (uri == null) return null
    val proj = arrayOf(MediaStore.MediaColumns.DATA)
    var path: String? = null
    val cursor: Cursor? = contentResolver.query(uri, proj, null, null, null)
    if (cursor != null && cursor.count != 0) {
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        cursor.moveToFirst()
        path = cursor.getString(columnIndex)
    }
    cursor?.close()
    return path ?: FileUtils(this).getPath(uri)
}

fun Context.shareMedia(media: Media) {
    ShareCompat
        .IntentBuilder(this)
        .setType(if (media.duration != null) "video/*" else "image/*")
        .addStream(FileProvider.getUriForFile(this, Constants.AUTHORITY, File(media.path)))
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
        shareCompat.addStream(
            FileProvider.getUriForFile(
                this, Constants.AUTHORITY, File(it.path)
            )
        )
    }
    shareCompat.startChooser()
}

fun Context.toggleFavorite(media: Media): Int {
    val value = media.favorite == 0
    val selection = MediaStore.MediaColumns._ID
    val selectionArgs = arrayOf(media.id.toString())
    val favoriteToggle = ContentValues().apply {
        put(MediaStore.MediaColumns.IS_FAVORITE, if (value) "1" else "0")
    }
    return try {
        contentResolver.getMediaUri(media)
            ?.let { contentResolver.update(it, favoriteToggle, selection, selectionArgs) } ?: -1
    } catch (e: NullPointerException) {
        Log.d("Gallery", "Failed to update ${media.path}")
        return -1
    }
}

fun Context.toggleFavorite(mediaList: List<Media>) {
    for (media in mediaList) {
        toggleFavorite(media)
    }
}

fun Context.trashMedia(
    result: ActivityResultLauncher<IntentSenderRequest>,
    mediaList: List<Media>,
    trash: Boolean = true
) {
    val intentSender = MediaStore.createTrashRequest(
        contentResolver,
        mediaList.map {
            contentResolver.getMediaUri(it)
        },
        trash
    ).intentSender
    val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
        .setFillInIntent(null)
        .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
        .build()
    result.launch(senderRequest)
}

fun Context.deleteMedia(
    result: ActivityResultLauncher<IntentSenderRequest>,
    mediaList: List<Media>
) {
    val intentSender = MediaStore.createDeleteRequest(
        contentResolver,
        mediaList.map {
            contentResolver.getMediaUri(it)
        }).intentSender
    val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
        .setFillInIntent(null)
        .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
        .build()
    result.launch(senderRequest)
}