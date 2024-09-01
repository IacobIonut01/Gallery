package com.dot.gallery.feature_node.domain.util

import android.net.Uri
import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
}