package com.dot.gallery.core

import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.domain.model.AlbumState
import com.dot.gallery.domain.model.CollectionWithCount
import com.dot.gallery.domain.model.GeoMedia
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.model.ImageEmbedding
import com.dot.gallery.domain.model.LocationMedia
import com.dot.gallery.domain.model.LockedAlbum
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.MergedSubfolderAlbum
import com.dot.gallery.domain.model.PinnedAlbum
import com.dot.gallery.domain.model.TimelineSettings
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.model.Vault
import com.dot.gallery.domain.model.VaultState
import com.dot.gallery.domain.util.MediaGroupType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MediaDistributor {

    /**
     * Pull-to-refresh
     */
    val isRefreshing: StateFlow<Boolean>
    suspend fun invalidate()

    /**
     * Common
     */
    val hasPermission: MutableStateFlow<Boolean>
    val dateFormatsFlow: StateFlow<Triple<String, String, String>>
    var groupByMonth: Boolean
    var groupByYear: Boolean
    val groupSimilarMedia: StateFlow<Boolean>
    val enabledGroupTypes: StateFlow<Set<MediaGroupType>>
    val mergeAlbumsByName: StateFlow<Boolean>

    /**
     * Settings
     */
    val settingsFlow: StateFlow<TimelineSettings?>

    /**
     * Albums
     */
    val albumsFlow: StateFlow<AlbumState>
    val blacklistedAlbumsFlow: StateFlow<List<IgnoredAlbum>>
    val pinnedAlbumsFlow: StateFlow<List<PinnedAlbum>>
    val lockedAlbumsFlow: StateFlow<List<LockedAlbum>>
    val mergedSubfolderAlbumsFlow: StateFlow<List<MergedSubfolderAlbum>>

    /**
     * Media
     */
    val timelineMediaFlow: SharedFlow<MediaState<UriMedia>>
    fun albumTimelineMediaFlow(
        albumId: Long,
        loadMode: AlbumMediaLoadMode = AlbumMediaLoadMode.Progressive
    ): Flow<MediaState<UriMedia>>
    val favoritesMediaFlow: SharedFlow<MediaState<UriMedia>>
    val trashMediaFlow: SharedFlow<MediaState<UriMedia>>

    /**
     * Optimistic mutations: media ids confirmed for trash/delete/restore are hidden from the
     * media flows until the underlying source catches up, and favorite toggles are applied
     * over the source the same way. Both self-heal — a pending id that reappears in a source
     * after being observed absent (e.g. a trash restore) is un-marked, overrides the source
     * already reflects are dropped, and surviving entries expire after a short TTL.
     */
    val pendingRemovalIds: StateFlow<Set<Long>>
    val favoriteOverrides: StateFlow<Map<Long, Boolean>>
    fun markPendingRemoval(
        ids: Collection<Long>,
        scope: PendingRemovalScope = PendingRemovalScope.EVERYWHERE
    )
    fun unmarkPendingRemoval(ids: Collection<Long>)
    fun setFavoriteOverride(mediaId: Long, favorite: Boolean)
    fun clearFavoriteOverride(mediaId: Long)

    /**
     * Cloud Sync States (media id → SyncState)
     */
    val cloudSyncStates: StateFlow<Map<Long, SyncState>>

    /**
     * Media Metadata
     */
    val metadataFlow: Flow<MediaMetadataState>
    val locationsMediaFlow: Flow<List<LocationMedia>>
    val geoMediaFlow: Flow<List<GeoMedia>>

    /**
     * Vault
     */
    val vaultsMediaFlow: StateFlow<VaultState>
    fun vaultMediaFlow(vault: Vault?): StateFlow<MediaState<UriMedia>>

    /**
     * Collections
     */
    val collectionsFlow: StateFlow<List<CollectionWithCount>>
    val collectionAlbumIdsFlow: StateFlow<Set<Long>>
    fun collectionAlbumIdsInCollection(collectionId: Long): Flow<List<Long>>
    fun collectionMediaFlow(collectionId: Long): StateFlow<MediaState<UriMedia>>

    /**
     * Search
     */
    val imageEmbeddingsFlow: StateFlow<List<ImageEmbedding>>


    fun locationBasedMedia(
        gpsLocationNameCity: String,
        gpsLocationNameCountry: String,
        latitude: Double?,
        longitude: Double?,
        additionalMediaIds: Flow<Set<Long>>,
    ): Flow<MediaState<UriMedia>>
}

fun MediaDistributor.restorableAlbumTimelineMediaFlow(
    albumId: Long
): Flow<MediaState<UriMedia>> = albumTimelineMediaFlow(
    albumId = albumId,
    loadMode = AlbumMediaLoadMode.Complete
)