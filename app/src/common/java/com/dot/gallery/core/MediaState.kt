/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.os.Parcel
import android.os.Parcelable
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media

data class MediaState(
    val isLoading: Boolean = false,
    val media: List<Media> = emptyList(),
    val error: String = ""
) : Parcelable {
    constructor(source: Parcel) : this(
        1 == source.readInt(),
        source.createTypedArrayList(Media.CREATOR)!!,
        source.readString()!!
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt((if (isLoading) 1 else 0))
        writeTypedList(media)
        writeString(error)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MediaState> = object : Parcelable.Creator<MediaState> {
            override fun createFromParcel(source: Parcel): MediaState = MediaState(source)
            override fun newArray(size: Int): Array<MediaState?> = arrayOfNulls(size)
        }
    }
}


data class AlbumState(
    val isLoading: Boolean = false,
    val albums: List<Album> = emptyList(),
    val error: String = ""
) : Parcelable {
    constructor(source: Parcel) : this(
        1 == source.readInt(),
        source.createTypedArrayList(Album.CREATOR)!!,
        source.readString()!!
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt((if (isLoading) 1 else 0))
        writeTypedList(albums)
        writeString(error)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AlbumState> = object : Parcelable.Creator<AlbumState> {
            override fun createFromParcel(source: Parcel): AlbumState = AlbumState(source)
            override fun newArray(size: Int): Array<AlbumState?> = arrayOfNulls(size)
        }
    }
}