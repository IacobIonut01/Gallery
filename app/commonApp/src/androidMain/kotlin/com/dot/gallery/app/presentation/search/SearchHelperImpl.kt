package com.dot.gallery.app.presentation.search

import com.dot.gallery.domain.util.dot

import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.feature.classifier.ui.search.SearchHelper
import com.dot.gallery.app.presentation.search.helpers.SearchVisionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.core.logging.printDebug



class SearchHelperImpl @Inject constructor(
    private val modelManager: ModelManager,
) : SearchHelper {

    override val isAvailable: Boolean get() = modelManager.isReady(ModelGroup.SEARCH)

    private val helper by lazy { SearchVisionHelper(modelManager) }

    override fun sortByCosineDistance(
        searchEmbedding: FloatArray,
        imageEmbeddingsList: List<FloatArray>,
        imageIdxList: List<Long>
    ): List<Pair<Long, Float>> {
        val distances = LinkedHashMap<Long, Float>()
        for (i in imageEmbeddingsList.indices) {
            val dist = searchEmbedding.dot(imageEmbeddingsList[i])
            distances[imageIdxList[i]] = dist
        }
        return distances.toList()
            .filter { it.second >= SearchVisionHelper.threshold }
            .sortedByDescending { (k, v) -> v }
            .map {
                printDebug(it)
                it
            }
    }

    override fun setupTextSession(): OrtSession = helper.setupTextSession()

    override suspend fun getTextEmbedding(session: OrtSession, text: String): FloatArray = withContext(Dispatchers.IO) {
        helper.getTextEmbedding(session, text)
    }

    override fun setupVisionSession(): OrtSession = helper.setupVisionSession()

    override suspend fun getImageEmbedding(session: OrtSession, bitmap: Bitmap): FloatArray = withContext(Dispatchers.IO) {
        helper.getImageEmbedding(session, bitmap)
    }

}