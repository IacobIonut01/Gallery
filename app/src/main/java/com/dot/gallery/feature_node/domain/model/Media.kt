package com.dot.gallery.feature_node.domain.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.dot.gallery.feature_node.presentation.util.getDate
import kotlinx.parcelize.Parcelize
import java.util.concurrent.TimeUnit

@Parcelize
sealed class MediaItem : Parcelable {
    abstract val key: String

    data class Header(
        override val key: String,
        val text: String,
    ) : MediaItem()

    sealed class MediaViewItem : MediaItem() {

        abstract val media: Media

        @Parcelize
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
    val selected: Boolean = false
) : Parcelable

class InvalidMediaException(message: String) : Exception(message)
