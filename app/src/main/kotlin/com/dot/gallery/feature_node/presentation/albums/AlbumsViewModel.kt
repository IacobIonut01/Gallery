package com.dot.gallery.feature_node.presentation.albums

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.R
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.FilterKind
import com.dot.gallery.core.presentation.components.FilterOption
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.AlbumGroup
import com.dot.gallery.feature_node.domain.model.AlbumGroupMember
import com.dot.gallery.feature_node.domain.model.AlbumGroupWithAlbums
import com.dot.gallery.feature_node.domain.model.AlbumSection
import com.dot.gallery.feature_node.domain.model.AlbumSectionMember
import com.dot.gallery.feature_node.domain.model.AlbumSectionType
import com.dot.gallery.feature_node.domain.model.AlbumSectionWithAlbums
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.domain.model.LockedAlbum
import com.dot.gallery.feature_node.domain.model.MergedSubfolderAlbum
import com.dot.gallery.feature_node.domain.model.PinnedAlbum
import com.dot.gallery.feature_node.domain.model.TimelineSettings
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.presentation.util.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val cloudMediaDao: CloudMediaDao,
    private val providerRegistry: ProviderRegistry
) : ViewModel() {

    val sectionsFlow = repository.getAllAlbumSections()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onAlbumClick(navigate: (String) -> Unit): (Album) -> Unit = { album ->
        navigate(Screen.AlbumViewScreen.route + "?albumId=${album.id}&albumName=${album.label}")
    }

    val onAlbumLongClick: (Album) -> Unit = { album ->
        toggleAlbumPin(album, !album.isPinned)
    }
    
    fun addAlbumToIgnored(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            // Get existing ignored albums to generate unique label
            val existingIgnored = repository.getBlacklistedAlbumsAsync()
            val existingCount = existingIgnored.count { it.label?.startsWith("Single") == true }
            
            var labelNumber = existingCount + 1
            var generatedLabel = "Single #$labelNumber"
            while (existingIgnored.any { it.label == generatedLabel }) {
                labelNumber++
                generatedLabel = "Single #$labelNumber"
            }
            
            repository.addBlacklistedAlbum(
                IgnoredAlbum(
                    id = album.id,
                    label = generatedLabel,
                    location = IgnoredAlbum.ALBUMS_AND_TIMELINE,
                    matchedAlbums = listOf(album.label)
                )
            )
        }
    }

    fun moveAlbumToTrash(result: ActivityResultLauncher<IntentSenderRequest>, album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            if (album.relativePath.startsWith("cloud/")) {
                // Cloud album: delete all media in this album from the cloud provider
                val providerName = album.relativePath.removePrefix("cloud/").split("/").firstOrNull() ?: return@launch
                val providerType = try { ProviderType.valueOf(providerName) } catch (_: Exception) { return@launch }
                val provider = providerRegistry.get(providerType) as? RemoteMediaProvider ?: return@launch
                val cloudMedia = cloudMediaDao.getByProvider(providerType).firstOrNull() ?: emptyList()
                val albumMedia = cloudMedia.filter { it.relativePath.contains(album.label) }
                albumMedia.forEach { entity ->
                    provider.deleteAsset(entity.remoteId)
                    cloudMediaDao.delete(entity.remoteId, providerType)
                }
            } else {
                val response = repository.getMediaByAlbumId(album.id).firstOrNull()
                val data = response?.data ?: emptyList()
                repository.trashMedia(result, data, true)
            }
        }
    }

    @Composable
    fun rememberFilters(): SnapshotStateList<FilterOption> {
        val lastValue by Settings.Album.rememberLastSort()
        return remember(lastValue) {
            mutableStateListOf(
                FilterOption(
                    titleRes = R.string.filter_type_date,
                    filterKind = FilterKind.DATE,
                    onClick = { albumOrder = it }
                ),
                FilterOption(
                    titleRes = R.string.filter_type_name,
                    filterKind = FilterKind.NAME,
                    onClick = { albumOrder = it }
                )
            )
        }
    }

    private fun toggleAlbumPin(album: Album, isPinned: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isPinned) {
                repository.insertPinnedAlbum(PinnedAlbum(album.id))
            } else {
                repository.removePinnedAlbum(PinnedAlbum(album.id))
            }
        }
    }

    fun toggleAlbumLock(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            if (album.isLocked) {
                repository.removeLockedAlbum(LockedAlbum(album.id))
            } else {
                repository.insertLockedAlbum(LockedAlbum(album.id))
            }
        }
    }

    fun toggleMergeSubfolders(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            val merged = MergedSubfolderAlbum(album.id)
            val existing = repository.getMergedSubfolderAlbums().firstOrNull() ?: emptyList()
            if (existing.any { it.id == album.id }) {
                repository.removeMergedSubfolderAlbum(merged)
            } else {
                repository.insertMergedSubfolderAlbum(merged)
            }
        }
    }

    private val settingsFlow = repository.getTimelineSettings()
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, TimelineSettings())


    private var albumOrder: MediaOrder
        get() = settingsFlow.value?.albumMediaOrder ?: MediaOrder.Date(OrderType.Descending)
        set(value) {
            viewModelScope.launch(Dispatchers.IO) {
                settingsFlow.value?.copy(albumMediaOrder = value)?.let {
                    repository.updateTimelineSettings(it)
                }
            }
        }

    // ============ Album Groups ============

    fun onGroupClick(navigate: (String) -> Unit): (AlbumGroupWithAlbums) -> Unit = { group ->
        navigate(Screen.AlbumGroupViewScreen.groupId(group.group.id))
    }

    fun createGroup(name: String, albumIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val groupId = repository.insertAlbumGroup(AlbumGroup(label = name))
            albumIds.forEach { albumId ->
                repository.addAlbumToGroup(AlbumGroupMember(groupId = groupId, albumId = albumId))
            }
        }
    }

    fun renameGroup(groupId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getAlbumGroupAsync(groupId) ?: return@launch
            repository.updateAlbumGroup(existing.copy(label = newName))
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlbumGroup(groupId)
        }
    }

    fun addAlbumToGroup(groupId: Long, albumId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addAlbumToGroup(AlbumGroupMember(groupId = groupId, albumId = albumId))
        }
    }

    fun removeAlbumFromGroup(groupId: Long, albumId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeAlbumFromGroup(AlbumGroupMember(groupId = groupId, albumId = albumId))
        }
    }

    // ============ Collections ============

    fun createCollection(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCollection(
                com.dot.gallery.feature_node.domain.model.Collection(label = name)
            )
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCollection(collectionId)
        }
    }

    fun toggleCollectionPin(collectionId: Long, isPinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleCollectionPinned(collectionId, isPinned)
        }
    }

    fun renameCollection(collectionId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCollectionLabel(collectionId, newName)
        }
    }

    fun createCollectionWithAlbums(name: String, albumIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val collectionId = repository.insertCollection(
                com.dot.gallery.feature_node.domain.model.Collection(label = name)
            )
            repository.addAlbumsToCollection(collectionId, albumIds)
            for (albumId in albumIds) {
                repository.getMediaByAlbumId(albumId)
                    .firstOrNull()
                    ?.data
                    ?.map { it.id }
                    ?.let { mediaIds ->
                        repository.addMediaListToCollection(collectionId, mediaIds)
                    }
            }
        }
    }

    // ============ Album Sections ============

    fun ensureDefaultSections() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getAlbumSectionCount()
            if (count > 0) return@launch
            repository.insertAlbumSection(
                AlbumSection(
                    label = "Common",
                    type = AlbumSectionType.COMMON.value,
                    displayOrder = 0
                )
            )
            repository.insertAlbumSection(
                AlbumSection(
                    label = "Apps",
                    type = AlbumSectionType.APPS.value,
                    displayOrder = 1
                )
            )
            repository.insertAlbumSection(
                AlbumSection(
                    label = "Other",
                    type = AlbumSectionType.UNCATEGORIZED.value,
                    displayOrder = 2
                )
            )
        }
    }

    fun createSection(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxOrder = repository.getAllAlbumSectionsAsync().maxOfOrNull { it.displayOrder } ?: 0
            repository.insertAlbumSection(
                AlbumSection(
                    label = name,
                    type = AlbumSectionType.CUSTOM.value,
                    displayOrder = maxOrder + 1
                )
            )
        }
    }

    fun renameSection(sectionId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getAlbumSectionAsync(sectionId) ?: return@launch
            repository.updateAlbumSection(existing.copy(label = newName))
        }
    }

    fun deleteSection(sectionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlbumSection(sectionId)
        }
    }

    fun toggleSectionVisibility(sectionId: Long, visible: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSectionVisibility(sectionId, visible)
        }
    }

    fun toggleSectionExpanded(sectionId: Long, expanded: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSectionExpanded(sectionId, expanded)
        }
    }

    fun moveAlbumToSection(albumId: Long, sectionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeAlbumFromAllSections(albumId)
            repository.addAlbumToSection(AlbumSectionMember(sectionId = sectionId, albumId = albumId))
        }
    }

    fun resetAlbumSectionOverride(albumId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeAlbumFromAllSections(albumId)
        }
    }

    fun reorderSections(orderedIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            orderedIds.forEachIndexed { index, sectionId ->
                repository.updateSectionDisplayOrder(sectionId, index)
            }
        }
    }

    fun addAlbumsToCollection(collectionId: Long, albumIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addAlbumsToCollection(collectionId, albumIds)
            for (albumId in albumIds) {
                repository.getMediaByAlbumId(albumId)
                    .firstOrNull()
                    ?.data
                    ?.map { it.id }
                    ?.let { mediaIds ->
                        repository.addMediaListToCollection(collectionId, mediaIds)
                    }
            }
        }
    }

}