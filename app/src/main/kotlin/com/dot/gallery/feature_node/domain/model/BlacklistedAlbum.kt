package com.dot.gallery.feature_node.domain.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "blacklist")
@Parcelize
data class BlacklistedAlbum(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val label: String
): Parcelable
