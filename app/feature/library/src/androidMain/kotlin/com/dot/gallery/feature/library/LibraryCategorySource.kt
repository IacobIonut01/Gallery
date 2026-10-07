package com.dot.gallery.feature.library

import com.dot.gallery.data.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton



@Singleton
class LibraryCategorySource @Inject constructor(
    repository: MediaRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryMedia>?> = repository.getTopCategories(5)
        .flatMapLatest { categories ->
            repository.getCategoryThumbnailMedia(
                categories.mapNotNull { it.thumbnailMediaId }
            ).map { media ->
                val byId = media.associateBy { it.id }
                categories.map { category ->
                    CategoryMedia(
                        category,
                        category.thumbnailMediaId?.let { id -> byId[id] }
                    )
                }
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

}
