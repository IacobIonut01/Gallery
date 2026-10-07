package com.dot.gallery.feature.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudAlbumIdentity
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.MediaDistributor
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.dot.gallery.domain.model.Album
import com.dot.gallery.core.presentation.util.remove



/**
 * Album-management actions for a cloud album open in the album view: rename,
 * delete, and remove-members. Calls are routed per-account through
 * [CloudRepository]; each successful mutation invalidates the distributor so
 * the album list and open timeline re-publish.
 */
class CloudAlbumManageViewModel @Inject constructor(
    private val cloudRepository: CloudRepository,
    private val distributor: MediaDistributor
) : ViewModel() {

    suspend fun renameAlbum(identity: CloudAlbumIdentity, newName: String): Result<CloudAlbum> =
        cloudRepository.renameAlbum(
            type = identity.providerType,
            configId = identity.serverConfigId,
            remoteAlbumId = identity.remoteId,
            newName = newName
        ).also { if (it.isSuccess) refresh() }

    suspend fun deleteAlbum(identity: CloudAlbumIdentity): Result<Unit> =
        cloudRepository.deleteRemoteAlbum(
            type = identity.providerType,
            configId = identity.serverConfigId,
            remoteAlbumId = identity.remoteId
        ).also { if (it.isSuccess) refresh() }

    suspend fun removeMembers(
        identity: CloudAlbumIdentity,
        remoteIds: List<String>
    ): Result<Unit> =
        cloudRepository.removeFromAlbum(
            type = identity.providerType,
            configId = identity.serverConfigId,
            remoteAlbumId = identity.remoteId,
            assetIds = remoteIds
        ).also { if (it.isSuccess) refresh() }

    private fun refresh() {
        viewModelScope.launch { distributor.invalidate() }
    }
}
