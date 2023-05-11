/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.model

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.concurrent.TimeUnit

@Parcelize
sealed class MediaItem : Parcelable {
    abstract val key: String

    data class Header(
        override val key: String,
        val text: String,
        val data: List<Media>
    ) : MediaItem()

    @Parcelize
    sealed class MediaViewItem : MediaItem() {

        abstract val media: Media

        data class Loaded(
            override val key: String,
            override val media: Media,
        ) : MediaViewItem()
    }
}

val Any.isHeaderKey: Boolean
    get() = this is String && this.startsWith("header")

@Parcelize
data class Media(
    val id: Long = 0,
    val label: String,
    val uri: Uri,
    val path: String,
    val albumID: Long,
    val albumLabel: String,
    val timestamp: Long,
    val mimeType: String,
    val orientation: Int,
    val favorite: Int,
    val trashed: Int,
    val duration: String? = null,
    var selected: Boolean = false
) : Parcelable {
    fun formatTime(): String {
        val timestamp = duration?.toLong() ?: return ""
        return String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(timestamp),
            TimeUnit.MILLISECONDS.toSeconds(timestamp) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timestamp))
        )
    }

}

@Parcelize
data class Album(
    val id: Long = 0,
    val label: String,
    val pathToThumbnail: String,
    val timestamp: Long,
    var count: Long = 0,
    val selected: Boolean = false,
    val isPinned: Boolean = false,
) : Parcelable

@Entity(tableName = "pinned_table")
data class PinnedAlbum(
    @PrimaryKey(autoGenerate = false)
    val id: Long
)

class InvalidMediaException(message: String) : Exception(message)
