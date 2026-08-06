package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.ImageEmbedding
import kotlinx.coroutines.flow.Flow

data class ImageEmbeddingHeader(
    val id: Long,
    val date: Long,
    val resultRevision: String,
    val embeddingBytes: Int
)

@Dao
interface ImageEmbeddingDao {
    @Upsert
    suspend fun addImageEmbedding(imageEmbedding: ImageEmbedding)

    @Query("SELECT * FROM image_embeddings WHERE id = :id LIMIT 1")
    suspend fun getRecord(id: Long): ImageEmbedding?

    @Query("SELECT * FROM image_embeddings")
    fun getRecords(): Flow<List<ImageEmbedding>>

    @Query("SELECT id, date, resultRevision, length(embedding) AS embeddingBytes FROM image_embeddings")
    suspend fun getHeaders(): List<ImageEmbeddingHeader>

    @Query("SELECT id FROM image_embeddings")
    suspend fun getIds(): List<Long>

    @Query("SELECT COUNT(*) FROM image_embeddings")
    suspend fun count(): Int

    @Query("UPDATE image_embeddings SET resultRevision = :revision WHERE id = :id")
    suspend fun updateResultRevision(id: Long, revision: String): Int

    @Query(
        """
        DELETE FROM image_embeddings
        WHERE NOT EXISTS (SELECT 1 FROM media WHERE media.id = image_embeddings.id)
          AND NOT EXISTS (SELECT 1 FROM cloud_media WHERE cloud_media.globalMediaId = image_embeddings.id)
        """
    )
    suspend fun deleteOrphans(): Int
}