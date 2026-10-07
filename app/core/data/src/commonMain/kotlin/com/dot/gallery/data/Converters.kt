package com.dot.gallery.data

import androidx.room3.ColumnTypeConverter
import com.dot.gallery.domain.model.CaptureTimeOrigin
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.uri.Uri
import com.dot.gallery.domain.uri.parseUri
import com.dot.gallery.domain.util.MediaOrder
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

object Converters {
    @ColumnTypeConverter
    fun toString(value: String?): List<String> = Json.decodeFromString(value ?: "[]")

    @ColumnTypeConverter
    fun fromList(list: List<String?>?): String = Json.encodeToString(list ?: emptyList())

    @ColumnTypeConverter
    fun toUri(value: String): Uri = parseUri(value)

    @ColumnTypeConverter
    fun fromUri(uri: Uri): String = uri.toString()

    @ColumnTypeConverter
    fun toMediaOrder(value: String): MediaOrder = Json.decodeFromString(value)

    @ColumnTypeConverter
    fun fromMediaOrder(mediaOrder: MediaOrder): String = Json.encodeToString(mediaOrder)

    @ColumnTypeConverter
    fun fromMedia(media: Media): String = Json.encodeToString(media)

    @ColumnTypeConverter
    fun toMedia(value: String): Media = Json.decodeFromString(value)

    @ColumnTypeConverter
    fun fromUUID(uuid: Uuid): String = uuid.toString()

    @ColumnTypeConverter
    fun toUUID(value: String): Uuid = Uuid.parse(value)

    @ColumnTypeConverter
    fun fromCaptureTimeOrigin(origin: CaptureTimeOrigin): String = origin.storedValue

    @ColumnTypeConverter
    fun toCaptureTimeOrigin(value: String): CaptureTimeOrigin =
        CaptureTimeOrigin.fromStoredValue(value)

    @ColumnTypeConverter
    fun fromFloatArray(array: FloatArray): ByteArray = FloatVectorCodec.encode(array)

    @ColumnTypeConverter
    fun toFloatArray(value: ByteArray): FloatArray = FloatVectorCodec.decode(value)

    @ColumnTypeConverter
    fun fromLongList(list: List<Long>?): String = Json.encodeToString(list ?: emptyList())

    @ColumnTypeConverter
    fun toLongList(value: String?): List<Long> = Json.decodeFromString(value ?: "[]")
}