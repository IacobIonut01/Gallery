package com.dot.gallery.feature_node.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.core.Resource
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.core.ml.ModelStatus
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.core.workers.startCategoryClassification
import com.dot.gallery.feature_node.data.data_source.CategoryWithMediaCount
import com.dot.gallery.feature_node.domain.model.LibraryIndicatorState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import com.dot.gallery.cloud.core.ConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class for category with its thumbnail media
 */
data class CategoryMedia(
    val category: CategoryWithMediaCount,
    val thumbnailMedia: Media.UriMedia?
)

data class CloudLibraryState(
    val hasCloud: Boolean = false,
    val archivedCount: Int = 0,
    val sharedLinkCount: Int = 0,
    val totalCloudCount: Int = 0,
    val people: List<PersonInfo> = emptyList(),
    val hasArchive: Boolean = false,
    val hasMemories: Boolean = false,
    val hasShareLink: Boolean = false,
    val hasPeople: Boolean = false,
    val hasMap: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val mediaDistributor: MediaDistributor,
    private val workManager: WorkManager,
    private val modelManager: ModelManager,
    private val cloudRepository: CloudRepository,
    private val providerRegistry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao,
    private val cloudServerConfigDao: CloudServerConfigDao
) : ViewModel() {

    val areAiFeaturesAvailable: Boolean get() = modelManager.areAiFeaturesAvailable

    val modelStatus: StateFlow<ModelStatus> = modelManager.status

    // === Cloud state ===
    private val _cloudState = MutableStateFlow(CloudLibraryState())
    val cloudState: StateFlow<CloudLibraryState> = _cloudState.asStateFlow()
    private var peopleJob: Job? = null
    private var sharedLinksJob: Job? = null

    init {
        // Pre-load cloud state from Room cache before auth completes
        viewModelScope.launch {
            preloadCachedCloudState()
        }
        // React to connection state changes so cloud items appear as soon as providers connect
        viewModelScope.launch {
            cloudRepository.connectionStates.collect { states ->
                val hasConnected = states.any { it.value == ConnectionState.CONNECTED }
                if (hasConnected) {
                    refreshCloudState()
                } else if (states.isNotEmpty()) {
                    _cloudState.value = CloudLibraryState(hasCloud = false)
                }
            }
        }
        // Re-fetch people when a name changes
        viewModelScope.launch {
            cloudRepository.peopleInvalidation.collect {
                refreshCloudState()
            }
        }
        // Also run once eagerly for already-connected providers
        refreshCloudState()
    }

    private suspend fun preloadCachedCloudState() {
        try {
            val activeConfigs = cloudServerConfigDao.getActive().firstOrNull()
                ?: emptyList()
            if (activeConfigs.isEmpty()) return
            val cached = cloudMediaDao.countCached()
            val archived = cloudMediaDao.countArchived()
            if (cached > 0 || archived > 0) {
                _cloudState.value = _cloudState.value.copy(
                    hasCloud = true,
                    hasArchive = true,
                    hasMemories = true,
                    hasShareLink = true,
                    hasPeople = true,
                    hasMap = true,
                    archivedCount = archived,
                    totalCloudCount = cached
                )
            }
        } catch (_: Exception) { }
    }

    fun refreshCloudState() {
        val providers = providerRegistry.getRemoteProviders()
        val hasCloud = providers.any { it.isAvailable }
        if (!hasCloud) {
            // Don't reset cached state if we have pre-loaded data and providers are still initializing
            if (!_cloudState.value.hasCloud) {
                _cloudState.value = CloudLibraryState(hasCloud = false)
            }
            return
        }
        val allCaps = providers.flatMap { it.capabilities }.toSet()
        _cloudState.value = _cloudState.value.copy(
            hasCloud = true,
            hasArchive = ProviderCapability.ARCHIVE in allCaps,
            hasMemories = ProviderCapability.MEMORIES in allCaps,
            hasShareLink = ProviderCapability.SHARE_LINK in allCaps,
            hasPeople = ProviderCapability.PEOPLE in allCaps,
            hasMap = ProviderCapability.MAP in allCaps
        )
        viewModelScope.launch {
            try {
                val archived = cloudMediaDao.countArchived()
                val total = cloudMediaDao.countCached()
                _cloudState.value = _cloudState.value.copy(
                    archivedCount = archived,
                    totalCloudCount = total
                )
            } catch (_: Exception) { }
        }
        if (ProviderCapability.PEOPLE in allCaps) {
            peopleJob?.cancel()
            peopleJob = viewModelScope.launch {
                cloudRepository.getAllPeople().collect { resource ->
                    if (resource is Resource.Success) {
                        _cloudState.value = _cloudState.value.copy(
                            people = resource.data ?: emptyList()
                        )
                    }
                }
            }
        }
        if (ProviderCapability.SHARE_LINK in allCaps) {
            val type = providers.first { it.isAvailable }.providerType
            sharedLinksJob?.cancel()
            sharedLinksJob = viewModelScope.launch {
                cloudRepository.getSharedLinks(type).collect { resource ->
                    if (resource is Resource.Success) {
                        _cloudState.value = _cloudState.value.copy(
                            sharedLinkCount = resource.data?.size ?: 0
                        )
                    }
                }
            }
        }
    }

    val locations = mediaDistributor.locationsMediaFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val geoMedia = mediaDistributor.geoMediaFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val indicatorState = combine(
        if (SdkCompat.supportsTrash) mediaDistributor.trashMediaFlow else flowOf(MediaState()),
        if (SdkCompat.supportsFavorites) mediaDistributor.favoritesMediaFlow else flowOf(MediaState())
    ) { trashed, favorites ->
        LibraryIndicatorState(
            trashCount = trashed.media.size,
            favoriteCount = favorites.media.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), LibraryIndicatorState())

    // New category system - top categories for library display with thumbnails
    private val topCategoriesRaw = repository.getTopCategories(5)
    
    val topCategories = combine(
        topCategoriesRaw,
        mediaDistributor.timelineMediaFlow
    ) { categories, mediaState ->
        val mediaMap = mediaState.media.associateBy { it.id }
        categories.map { category ->
            CategoryMedia(
                category = category,
                thumbnailMedia = category.thumbnailMediaId?.let { mediaMap[it] }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    // Total count of categories with media (for the "See all" indicator)
    val totalCategoryCount = repository.getCategoryCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    // Legacy classification system (for backwards compatibility)
    val classifiedCategories = repository.getClassifiedCategories()
        .map { if (it.isNotEmpty()) it.distinct() else it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val mostPopularCategory = repository.getClassifiedMediaByMostPopularCategory()
        .map { it.groupBy { it.category!! }.toSortedMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    /**
     * Start the category classification using the new CLIP-based system
     */
    fun startClassification() {
        workManager.startCategoryClassification()
    }

}