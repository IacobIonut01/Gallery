/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.cloud.core.CloudUri
import com.dot.gallery.cloud.data.repository.CloudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.presentation.util.update


/**
 * Backs the media-viewer "backed up on" sheet. Deletes a single cloud copy from the
 * account that holds it; the cache row removal makes the timeline/backup sheet update
 * reactively (the local original is never touched).
 */
class CloudBackupInfoViewModel @Inject constructor(
    private val repository: CloudRepository
) : ViewModel() {

    private val _deletingIds = MutableStateFlow<Set<Long>>(emptySet())
    val deletingIds: StateFlow<Set<Long>> = _deletingIds.asStateFlow()

    fun deleteBackup(backup: UriMedia, onResult: (Boolean) -> Unit = {}) {
        val cloudUri = CloudUri.parse(backup.uri.toString())
        if (cloudUri == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            _deletingIds.value = _deletingIds.value + backup.id
            val result = repository.deleteAsset(
                type = cloudUri.providerType,
                configId = cloudUri.configId,
                remoteId = cloudUri.remoteId
            )
            _deletingIds.value = _deletingIds.value - backup.id
            onResult(result.isSuccess)
        }
    }
}
