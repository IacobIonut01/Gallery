package com.dot.gallery.domain.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "image_embeddings")
@Serializable
data class ImageEmbedding(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val date: Long,
    val embedding: FloatArray,
    @ColumnInfo(defaultValue = "''")
    val resultRevision: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageEmbedding) return false

        if (id != other.id) return false
        if (date != other.date) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (resultRevision != other.resultRevision) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + resultRevision.hashCode()
        return result
    }
}