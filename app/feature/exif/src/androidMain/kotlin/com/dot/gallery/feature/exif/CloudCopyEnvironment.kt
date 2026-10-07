package com.dot.gallery.feature.exif

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.CloudAccountRuntimeSettings
import com.dot.gallery.cloud.core.CloudAlbumIdentity
import com.dot.gallery.cloud.core.CloudRuntimeSettings
import com.dot.gallery.cloud.core.CloudUri
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.cloudMediaId
import com.dot.gallery.cloud.core.isUnsupportedSameAccountCloudMove
import com.dot.gallery.cloud.core.capabilities.RemoteAlbumWriteProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.sync.CloudAlbumTransferMode
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.feature.exif.ui.CloudCopyDestinationStatus
import com.dot.gallery.feature.exif.ui.CloudDestinationAccount
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.dot.gallery.core.ui.components.settings
import com.dot.gallery.core.util.or
import com.dot.gallery.cloud.core.matches
import com.dot.gallery.core.presentation.util.remove
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.offline.OfflineModeManager
import com.dot.gallery.cloud.sync.CloudAlbumCopyRequestStore
import com.dot.gallery.domain.util.isCloud
import com.dot.gallery.domain.uri.host
import com.dot.gallery.domain.uri.scheme
import com.dot.gallery.feature.exif.ui.cloudCopyDestinationStatus



data class CloudCopyEnvironment(
    val connectionStates: Map<Long, ConnectionState> = emptyMap(),
    val settingsByConfigId: Map<Long, CloudAccountRuntimeSettings> = emptyMap(),
    val accountsByConfigId: Map<Long, CloudDestinationAccount> = emptyMap(),
    val offline: Boolean = false
)