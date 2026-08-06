package com.dot.gallery.feature_node.domain.util

import android.net.Uri
import androidx.room.TypeConverter
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

object FloatVectorCodec {
    fun encode(values: FloatArray): ByteArray =
        ByteBuffer.allocate(values.size * Float.SIZE_BYTES).order(ByteOrder.BIG_ENDIAN).apply {
            values.forEach(::putFloat)
        }.array()

    fun decode(bytes: ByteArray): FloatArray {
        require(bytes.size % Float.SIZE_BYTES == 0) { "Invalid float vector byte count" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        return FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.float }
    }
}

object Converters {
    @TypeConverter
    fun toString(value: String?): List<String> = Json.decodeFromString(value ?: "[]")

    @TypeConverter
    fun fromList(list: List<String?>?): String = Json.encodeToString(list ?: emptyList())

    @TypeConverter
    fun toUri(value: String): Uri = Uri.parse(value)

    @TypeConverter
    fun fromUri(uri: Uri): String = uri.toString()

    @TypeConverter
    fun toMediaOrder(value: String): MediaOrder = Json.decodeFromString(value)

    @TypeConverter
    fun fromMediaOrder(mediaOrder: MediaOrder): String = Json.encodeToString(mediaOrder)

    @TypeConverter
    fun fromMedia(media: Media): String = Json.encodeToString(media)

    @TypeConverter
    fun toMedia(value: String): Media = Json.decodeFromString(value)

    @TypeConverter
    fun fromUUID(uuid: UUID): String = uuid.toString()

    @TypeConverter
    fun toUUID(value: String): UUID = UUID.fromString(value)

    @TypeConverter
    fun fromFloatArray(array: FloatArray): ByteArray = FloatVectorCodec.encode(array)

    @TypeConverter
    fun toFloatArray(value: ByteArray): FloatArray = FloatVectorCodec.decode(value)

    @TypeConverter
    fun fromLongList(list: List<Long>?): String = Json.encodeToString(list ?: emptyList())

    @TypeConverter
    fun toLongList(value: String?): List<Long> = Json.decodeFromString(value ?: "[]")
}