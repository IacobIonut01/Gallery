package com.dot.gallery.feature_node.presentation.util

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.FileProvider
import com.dot.gallery.R
import com.dot.gallery.feature_node.data.data_types.getMediaDeleteUri
import com.dot.gallery.feature_node.domain.model.Media
import java.io.File


fun Context.shareImage(media: Media) {
    val share = Intent(Intent.ACTION_SEND)
    share.type = if (media.duration != null) "video/*" else "image/*"
    share.putExtra(
        Intent.EXTRA_STREAM,
        FileProvider.getUriForFile(this, "$packageName.provider", File(media.path))
    )
    startActivity(Intent.createChooser(share, getString(R.string.share_media)))
}

fun Context.deleteImage(
    deleteResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
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
    deleteResultLauncher.launch(senderRequest)
}