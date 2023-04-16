package com.dot.gallery.core

import android.net.Uri
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun uriToString(value: Uri): String = value.toString()

    @TypeConverter
    fun fromString(value: String): Uri = Uri.parse(value)
}