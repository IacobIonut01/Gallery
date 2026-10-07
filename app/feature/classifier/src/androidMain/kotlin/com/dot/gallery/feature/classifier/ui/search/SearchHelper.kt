package com.dot.gallery.feature.classifier.ui.search

import ai.onnxruntime.OrtSession
import android.graphics.Bitmap



interface SearchHelper {

    val isAvailable: Boolean

    fun sortByCosineDistance(
        searchEmbedding: FloatArray,
        imageEmbeddingsList: List<FloatArray>,
        imageIdxList: List<Long>
    ): List<Pair<Long, Float>>

    suspend fun getTextEmbedding(session: OrtSession, text: String): FloatArray

    fun setupTextSession(): OrtSession

    fun setupVisionSession(): OrtSession

    suspend fun getImageEmbedding(session: OrtSession, bitmap: Bitmap): FloatArray
}