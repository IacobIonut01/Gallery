/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif

import android.content.ContentUris
import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.Settings
import com.dot.gallery.core.sandbox.IsolatedMetadataParser
import com.dot.gallery.feature.exif.ui.MetadataViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

class MetadataViewViewModel @Inject constructor(
    private val isolatedParser: IsolatedMetadataParser,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(MetadataViewState())
    val state: StateFlow<MetadataViewState> = _state

    fun loadMetadata(mediaUri: String, isVideo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = MetadataViewState(isLoading = true)
            val uri = mediaUri.toUri()
            val directories = runCatching {
                val mode = Settings.Security.getMetadataIsolationMode(appContext)
                    .firstOrNull() ?: Settings.Security.METADATA_ISOLATION_SHARED
                val usePerFile = mode != Settings.Security.METADATA_ISOLATION_SHARED
                if (usePerFile) {
                    val mediaId = ContentUris.parseId(uri)
                    isolatedParser.parseRawMetadataPerFile(uri, isVideo, mediaId)
                } else {
                    isolatedParser.parseRawMetadata(uri, isVideo)
                }
            }.getOrElse { emptyList() }
            _state.value = MetadataViewState(
                isLoading = false,
                directories = directories
            )
        }
    }
}
