package com.dot.gallery.feature_node.data.data_source

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dot.gallery.feature_node.domain.model.ImageEmbedding
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageEmbeddingDao {
    @Upsert
    suspend fun addImageEmbedding(imageEmbedding: ImageEmbedding)

    @Query("SELECT * FROM image_embeddings WHERE id = :id LIMIT 1")
    suspend fun getRecord(id: Long): ImageEmbedding?

    @Query("SELECT * FROM image_embeddings")
    fun getRecords(): Flow<List<ImageEmbedding>>

    @Query(
        """
        DELETE FROM image_embeddings
        WHERE NOT EXISTS (SELECT 1 FROM media WHERE media.id = image_embeddings.id)
          AND NOT EXISTS (SELECT 1 FROM cloud_media WHERE cloud_media.globalMediaId = image_embeddings.id)
        """
    )
    suspend fun deleteOrphans(): Int
}