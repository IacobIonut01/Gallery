package com.dot.gallery.feature_node.presentation.util

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.dot.gallery.feature_node.data.data_types.getMediaDeleteUri
import com.dot.gallery.feature_node.domain.model.Media
import java.io.File

fun Context.shareMedia(media: Media) {
    ShareCompat
        .IntentBuilder(this)
        .setType(if (media.duration != null) "video/*" else "image/*")
        .addStream(FileProvider.getUriForFile(this, "$packageName.provider", File(media.path)))
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
        shareCompat.addStream(FileProvider.getUriForFile(this, "$packageName.provider", File(it.path)))
    }
    shareCompat.startChooser()
}

fun Context.trashImages(
    result: ActivityResultLauncher<IntentSenderRequest>,
    mediaList: List<Media>
) {
    val intentSender = MediaStore.createTrashRequest(
        contentResolver,
        mediaList.map {
            contentResolver.getMediaDeleteUri(it)
        },
        true
    ).intentSender
    val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
        .setFillInIntent(null)
        .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
        .build()
    result.launch(senderRequest)
}


fun Context.deleteImage(
    result: ActivityResultLauncher<IntentSenderRequest>,
    mediaList: ArrayList<Media>
) {
    val intentSender = MediaStore.createDeleteRequest(
        contentResolver,
        mediaList.map {
            contentResolver.getMediaDeleteUri(it)
        }).intentSender
    val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
        .setFillInIntent(null)
        .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
        .build()
    result.launch(senderRequest)
}