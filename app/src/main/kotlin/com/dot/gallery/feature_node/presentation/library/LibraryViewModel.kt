package com.dot.gallery.feature_node.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.core.Resource
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.core.ml.ModelStatus
import com.dot.gallery.core.smart.SmartScanScheduler
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.feature_node.data.data_source.CategoryWithMediaCount
import com.dot.gallery.feature_node.data.data_source.SmartScanFeature
import com.dot.gallery.feature_node.domain.model.LibraryIndicatorState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.presentation.location.MapGeoMediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val isConnected: Boolean = false,
    val connectedCapabilities: Set<ProviderCapability> = emptySet(),
    val hasCachedMedia: Boolean = false,
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

internal data class CloudLibraryAvailability(
    val hasCloud: Boolean,
    val isConnected: Boolean,
    val hasArchive: Boolean,
    val hasMemories: Boolean,
    val hasShareLink: Boolean,
    val hasPeople: Boolean,
    val hasMap: Boolean,
)

internal fun resolveCloudLibraryAvailability(
    hasConfiguredAccounts: Boolean,
    configuredCapabilities: Set<ProviderCapability>,
    isConnected: Boolean,
): CloudLibraryAvailability = CloudLibraryAvailability(
    hasCloud = hasConfiguredAccounts,
    isConnected = isConnected,
    hasArchive = ProviderCapability.ARCHIVE in configuredCapabilities,
    hasMemories = ProviderCapability.MEMORIES in configuredCapabilities,
    hasShareLink = ProviderCapability.SHARE_MANAGE in configuredCapabilities,
    hasPeople = ProviderCapability.PEOPLE in configuredCapabilities,
    hasMap = ProviderCapability.MAP in configuredCapabilities,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val mediaDistributor: MediaDistributor,
    private val smartScanScheduler: SmartScanScheduler,
    private val modelManager: ModelManager,
    private val cloudRepository: CloudRepository,
    private val providerRegistry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao,
    private val cloudServerConfigDao: CloudServerConfigDao,
    mapGeoMediaSource: MapGeoMediaSource,
) : ViewModel() {

    val areAiFeaturesAvailable: Boolean get() = modelManager.areAiFeaturesAvailable

    val modelStatus: StateFlow<ModelStatus> = modelManager.status(ModelGroup.SEARCH)

    // === Cloud state ===
    private val _cloudState = MutableStateFlow(CloudLibraryState())
    val cloudState: StateFlow<CloudLibraryState> = _cloudState.asStateFlow()
    private var peopleJob: Job? = null
    private var sharedLinksJob: Job? = null
    private var activeAccountCount: Int = 0

    init {
        // Configuration, advertised capabilities and connectivity are separate facts. In
        // particular, disconnecting an account must not make its supported features disappear.
        viewModelScope.launch {
            cloudServerConfigDao.getActive().collect { activeConfigs ->
                activeAccountCount = activeConfigs.size
                _cloudState.value = _cloudState.value.copy(hasCloud = activeConfigs.isNotEmpty())
                refreshCachedCloudState()
                refreshCloudState()
            }
        }
        viewModelScope.launch {
            providerRegistry.connectionStates.collect {
                refreshCloudState()
            }
        }
        // Re-fetch people when a name changes
        viewModelScope.launch {
            cloudRepository.peopleInvalidation.collect {
                refreshCloudState()
            }
        }
        // Always-on people collector: combines local (on-device) and cloud people providers, so
        // on-device Person grouping surfaces in the Library even when no cloud account exists.
        viewModelScope.launch {
            cloudRepository.getAllPeople().collect { resource ->
                if (resource is Resource.Success) {
                    val people = resource.data ?: emptyList()
                    _cloudState.value = _cloudState.value.copy(
                        people = people,
                        hasPeople = _cloudState.value.hasPeople || people.isNotEmpty()
                    )
                }
            }
        }
        // Also run once eagerly for already-connected providers
        refreshCloudState()
    }

    private suspend fun refreshCachedCloudState() {
        try {
            val cached = cloudMediaDao.countCached()
            val archived = cloudMediaDao.countArchived()
            _cloudState.value = _cloudState.value.copy(
                hasCachedMedia = cached > 0 || archived > 0,
                archivedCount = archived,
                totalCloudCount = cached,
            )
        } catch (_: Exception) { }
    }

    fun refreshCloudState() {
        // getRemoteProviders() intentionally returns only currently available providers. Library
        // feature availability instead comes from every registered/configured remote provider.
        val providers = providerRegistry.getAll().filterIsInstance<RemoteMediaProvider>()
        val allCaps = providers.flatMapTo(LinkedHashSet()) { it.capabilities }
        val connectedCaps = providers.asSequence()
            .filter { it.isAvailable }
            .flatMap { it.capabilities.asSequence() }
            .toSet()
        val availability = resolveCloudLibraryAvailability(
            hasConfiguredAccounts = activeAccountCount > 0 || providers.isNotEmpty(),
            configuredCapabilities = allCaps,
            isConnected = providerRegistry.connectionStates.value.values.any {
                it == ConnectionState.CONNECTED || it == ConnectionState.SYNCING
            },
        )
        _cloudState.value = _cloudState.value.copy(
            hasCloud = availability.hasCloud,
            isConnected = availability.isConnected,
            connectedCapabilities = connectedCaps,
            hasArchive = availability.hasArchive,
            hasMemories = availability.hasMemories,
            hasShareLink = availability.hasShareLink,
            hasPeople = _cloudState.value.people.isNotEmpty() || availability.hasPeople,
            hasMap = availability.hasMap,
        )
        viewModelScope.launch { refreshCachedCloudState() }
        if (availability.isConnected && ProviderCapability.PEOPLE in allCaps) {
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
        if (providers.any {
                it.isAvailable && ProviderCapability.SHARE_MANAGE in it.capabilities
            }
        ) {
            sharedLinksJob?.cancel()
            sharedLinksJob = viewModelScope.launch {
                val accounts = cloudServerConfigDao.getActive().first().filter { config ->
                    val provider = providerRegistry.getByConfigId(config.id)
                    provider?.isAvailable == true &&
                            ProviderCapability.SHARE_MANAGE in provider.capabilities
                }
                if (accounts.isEmpty()) return@launch
                combine(
                    accounts.map { config ->
                        cloudRepository.getSharedLinks(config.providerType, config.id)
                    }
                ) { resources ->
                    resources.sumOf { resource ->
                        (resource as? Resource.Success)?.data?.size ?: 0
                    }
                }.collect { count ->
                    _cloudState.value = _cloudState.value.copy(sharedLinkCount = count)
                }
            }
        }
    }

    val geoMedia = mapGeoMediaSource.mergedGeoMedia(
        localGeoMedia = mediaDistributor.geoMediaFlow,
        timelineMedia = mediaDistributor.timelineMediaFlow,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val locations = mapGeoMediaSource.mergedLocations(
        localLocations = mediaDistributor.locationsMediaFlow,
        geoMedia = geoMedia,
        timelineMedia = mediaDistributor.timelineMediaFlow,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        viewModelScope.launch { smartScanScheduler.manual(SmartScanFeature.CATEGORIES.bit) }
    }

}