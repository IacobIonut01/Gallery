/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.CloudUri
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SharedLinkInfo
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.cloud.core.capabilities.ShareLinkCapableProvider
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.data.dao.CloudServerConfigDao
import com.dot.gallery.data.dao.PersonDao
import com.dot.gallery.data.entity.CloudServerConfigEntity
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.core.Resource
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelStatus
import com.dot.gallery.core.presentation.components.util.hasMediaAccess
import com.dot.gallery.core.metrics.StartupTracer
import com.dot.gallery.core.startup.StartupCacheStamp
import com.dot.gallery.core.startup.fullReadPermissionMask
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.domain.model.GeoMedia
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.model.LibraryIndicatorState
import com.dot.gallery.domain.model.LocationMedia
import com.dot.gallery.domain.model.LockedAlbum
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.shouldIgnore
import com.dot.gallery.data.repository.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.security.MessageDigest
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.util.or
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.feature.library.location.MapGeoMediaSource
import com.dot.gallery.feature.library.ui.LibraryCategoryPreview
import com.dot.gallery.feature.library.ui.LibraryGeoPreview
import com.dot.gallery.feature.library.ui.LibrarySnapshot
import com.dot.gallery.feature.library.ui.LibraryViewport
import com.dot.gallery.cloud.core.LOCAL_PEOPLE_CONFIG_ID
import com.dot.gallery.feature.library.ui.resolveCloudLibraryAvailability
import com.dot.gallery.feature.library.ui.mergeLibraryViewport
import com.dot.gallery.domain.model.isCompleteForLibrary

class LibraryContentInputs(
    val lockedAlbums: Flow<List<LockedAlbum>>,
    val ignoredAlbums: Flow<List<IgnoredAlbum>>,
    val activeConfigs: Flow<List<CloudServerConfigEntity>>,
    val timelineMedia: Flow<MediaState<UriMedia>>,
    val localGeoMedia: Flow<List<GeoMedia>>,
    val localLocations: Flow<List<LocationMedia>>,
    val trashMedia: Flow<MediaState<UriMedia>>,
    val favoritesMedia: Flow<MediaState<UriMedia>>,
    val categories: Flow<List<CategoryMedia>?>,
    val categoryCount: Flow<Int>,
    val connectionStates: Flow<Map<Long, ConnectionState>>,
    val peopleInvalidation: Flow<Unit>,
    val hiddenPeopleCount: Flow<Int>,
    val faceDetectStatus: Flow<ModelStatus>,
    val providerByConfigId: (Long) -> MediaCapabilityProvider?,
    val sharedLinks: (ProviderType, Long) -> Flow<Resource<List<SharedLinkInfo>>>,
    val cachedCloudCounts: suspend () -> Pair<Int, Int>,
    val supportsTrash: Boolean,
    val supportsFavorites: Boolean,
    val hasMediaAccess: () -> Boolean,
    val fullReadPermissionMask: () -> Int?,
    val mergedGeo: (
        Flow<List<GeoMedia>>,
        Flow<MediaState<UriMedia>>
    ) -> Flow<List<GeoMedia>>,
    val mergedLocations: (
        Flow<List<LocationMedia>>,
        Flow<List<GeoMedia>>,
        Flow<MediaState<UriMedia>>
    ) -> Flow<List<LocationMedia>>,
    val readCache: suspend (String) -> LibrarySnapshot?,
    val writeCache: suspend (StartupCacheStamp?, String, LibrarySnapshot) -> Unit,
    val currentStamp: suspend () -> StartupCacheStamp?,
)