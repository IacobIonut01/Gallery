package com.dot.gallery.feature_node.domain.util

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Converters {
    @TypeConverter
    fun fromString(value: String?): List<String> = Json.decodeFromString(value ?: "[]")

    @TypeConverter
    fun fromList(list: List<String?>?): String = Json.encodeToString(list ?: emptyList())
}