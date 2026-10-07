package com.dot.gallery.domain.util

import com.dot.gallery.domain.model.EncryptedMedia
import com.dot.gallery.domain.model.EncryptedMedia2
import com.dot.gallery.domain.model.Media
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

@Suppress("UNCHECKED_CAST")
inline fun <reified T> fromKotlinByteArray(byteArray: ByteArray): T =
    Json.decodeFromString(byteArray.decodeToString())

inline fun <reified T> T.toKotlinByteArray() = Json.encodeToString(this).encodeToByteArray()

fun EncryptedMedia.migrate(uuid: Uuid): EncryptedMedia2 = EncryptedMedia2(
    id = id,
    label = label,
    uuid = uuid,
    path = path,
    timestamp = timestamp,
    mimeType = mimeType,
    duration = duration,
    trashed = trashed,
    favorite = favorite,
    albumID = albumID,
    albumLabel = albumLabel,
    relativePath = relativePath,
    fullDate = fullDate,
    size = size,
)

fun <T : Media> T.toEncryptedMedia(bytes: ByteArray): EncryptedMedia {
    return EncryptedMedia(
        id = id,
        label = label,
        bytes = bytes,
        path = path,
        timestamp = timestamp,
        mimeType = mimeType,
        duration = duration,
        trashed = trashed,
        favorite = favorite,
        albumID = albumID,
        albumLabel = albumLabel,
        relativePath = relativePath,
        fullDate = fullDate,
        size = size,
    )
}

fun <T : Media> T.toEncryptedMedia2(uuid: Uuid): EncryptedMedia2 {
    return EncryptedMedia2(
        id = id,
        label = label,
        uuid = uuid,
        path = path,
        timestamp = timestamp,
        mimeType = mimeType,
        duration = duration,
        trashed = trashed,
        favorite = favorite,
        albumID = albumID,
        albumLabel = albumLabel,
        relativePath = relativePath,
        fullDate = fullDate,
        size = size,
    )
}
