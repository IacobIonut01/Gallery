package com.dot.gallery.domain.model

import androidx.room3.Entity

@Entity(tableName = "media_version", primaryKeys = ["version"])
data class MediaVersion(
    val version: String
)
