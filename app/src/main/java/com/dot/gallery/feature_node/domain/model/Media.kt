package com.dot.gallery.feature_node.domain.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import java.util.concurrent.TimeUnit

sealed class MediaItem {
    abstract val key: String

    data class Header(
        override val key: String,
        val text: String,
    ) : MediaItem(), Parcelable {
        constructor(source: Parcel) : this(
            source.readString()!!,
            source.readString()!!
        )

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
            writeString(key)
            writeString(text)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<Header> = object : Parcelable.Creator<Header> {
                override fun createFromParcel(source: Parcel): Header = Header(source)
                override fun newArray(size: Int): Array<Header?> = arrayOfNulls(size)
            }
        }
    }

    sealed class MediaViewItem : MediaItem() {

        abstract val media: Media

        data class Loaded(
            override val key: String,
            override val media: Media,
        ) : MediaViewItem(), Parcelable {
            constructor(source: Parcel) : this(
                source.readString()!!,
                source.readParcelable<Media>(Media::class.java.classLoader)!!
            )

            override fun describeContents() = 0

            override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
                writeString(key)
                writeParcelable(media, 0)
            }

            companion object {
                @JvmField
                val CREATOR: Parcelable.Creator<Loaded> = object : Parcelable.Creator<Loaded> {
                    override fun createFromParcel(source: Parcel): Loaded = Loaded(source)
                    override fun newArray(size: Int): Array<Loaded?> = arrayOfNulls(size)
                }
            }
        }
    }
}

val Any.isHeaderKey: Boolean
    get() = this is String && this.startsWith("header")

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

    constructor(source: Parcel) : this(
        source.readLong(),
        source.readString()!!,
        source.readParcelable<Uri>(Uri::class.java.classLoader)!!,
        source.readString()!!,
        source.readLong(),
        source.readString()!!,
        source.readLong(),
        source.readString()!!,
        source.readInt(),
        source.readString(),
        1 == source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeLong(id)
        writeString(label)
        writeParcelable(uri, 0)
        writeString(path)
        writeLong(albumID)
        writeString(albumLabel)
        writeLong(timestamp)
        writeString(mimeType)
        writeInt(orientation)
        writeString(duration)
        writeInt((if (selected) 1 else 0))
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Media> = object : Parcelable.Creator<Media> {
            override fun createFromParcel(source: Parcel): Media = Media(source)
            override fun newArray(size: Int): Array<Media?> = arrayOfNulls(size)
        }
    }
}


data class Album(
    val id: Long = 0,
    val label: String,
    val pathToThumbnail: String,
    val timestamp: Long,
    var count: Long = 0,
    val selected: Boolean = false
) : Parcelable {
    constructor(source: Parcel) : this(
    source.readLong(),
    source.readString()!!,
    source.readString()!!,
    source.readLong(),
    source.readLong(),
    1 == source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeLong(id)
        writeString(label)
        writeString(pathToThumbnail)
        writeLong(timestamp)
        writeLong(count)
        writeInt((if (selected) 1 else 0))
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Album> = object : Parcelable.Creator<Album> {
            override fun createFromParcel(source: Parcel): Album = Album(source)
            override fun newArray(size: Int): Array<Album?> = arrayOfNulls(size)
        }
    }
}

class InvalidMediaException(message: String) : Exception(message)
