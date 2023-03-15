package com.dot.gallery.feature_node.domain.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

data class Media(
    val id: Long = 0,
    val label: String,
    val uri: Uri,
    val path: String,
    val albumID: Long,
    val timestamp: Long,
    val duration: String? = null,
    var selected: Boolean = false
)

data class Album(
    val id: Long = 0,
    val label: String,
    val path: String,
    val selected: Boolean = false
)

class InvalidMediaException(message: String): Exception(message)
