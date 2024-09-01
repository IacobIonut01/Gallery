/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.core.util

enum class MediaStoreBuckets {
    /**
     * Favorites album.
     */
    MEDIA_STORE_BUCKET_FAVORITES,

    /**
     * Trash album.
     */
    MEDIA_STORE_BUCKET_TRASH,

    /**
     * Timeline, contains all medias.
     */
    MEDIA_STORE_BUCKET_TIMELINE,

    /**
     * Reserved bucket ID for placeholders, throw an exception if this value is used.
     */
    MEDIA_STORE_BUCKET_PLACEHOLDER,

    /**
     * Timeline, contains only photos.
     */
    MEDIA_STORE_BUCKET_PHOTOS,

    /**
     * Timeline, contains only videos.
     */
    MEDIA_STORE_BUCKET_VIDEOS;

    val id = (-0x0000DEAD - ((ordinal + 1) shl 16)).toLong()
}