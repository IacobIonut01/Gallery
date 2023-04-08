package com.dot.gallery.feature_node.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.data.data_types.getMediaUri
import com.dot.gallery.feature_node.domain.model.Media

class MediaRepositorySystemImpl(
    private val context: Context
) : MediaRepositoryImpl(context) {
    private val contentResolver by lazy { context.contentResolver }

    override suspend fun toggleFavorite(media: Media): Int {
        val value = media.favorite == 0
        val selection = MediaStore.MediaColumns._ID + "=?"
        val selectionArgs = arrayOf(media.id.toString())
        val favoriteToggle = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_FAVORITE, if (value) "1" else "0")
        }
        return try {
            contentResolver.getMediaUri(media)
                ?.let { contentResolver.update(it, favoriteToggle, selection, selectionArgs) } ?: -1
        } catch (e: Exception) {
            Log.d(Constants.TAG, "Failed to update ${media.path}")
            e.printStackTrace()
            return -1
        }
    }

    override suspend fun toggleFavorite(mediaList: List<Media>) {
        for (media in mediaList) {
            toggleFavorite(media)
        }
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