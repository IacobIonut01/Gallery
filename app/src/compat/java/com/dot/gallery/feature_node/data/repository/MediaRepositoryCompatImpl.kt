package com.dot.gallery.feature_node.data.repository

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.dot.gallery.feature_node.data.data_types.getMediaUri
import com.dot.gallery.feature_node.domain.model.Media

class MediaRepositoryCompatImpl(
    private val context: Context
) : MediaRepositoryImpl(context) {
    private val contentResolver by lazy { context.contentResolver }

    override suspend fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean
    ) {
        val intentSender = MediaStore.createFavoriteRequest(
            contentResolver,
            mediaList.map {
                contentResolver.getMediaUri(it)
            },
            favorite
        )
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFillInIntent(null)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest)
    }

    override suspend fun trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        trash: Boolean
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

    override suspend fun deleteMedia(
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
}