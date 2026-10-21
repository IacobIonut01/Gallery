/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.SavedStateHandle
import androidx.media3.datasource.DataSource
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.domain.model.Media
import com.dot.gallery.feature.viewer.components.video.VideoPlayerViewModel
import com.github.panpf.zoomimage.subsampling.ImageSource


/**
 * App-provided runtime surface for the media viewer chain. The viewer composables live in
 * this module while the heavy lifting (DI component, cloud fetchers, ExoPlayer data source)
 * stays app-side — this interface is the bridge, provided once via [LocalViewerRuntime].
 */
interface ViewerRuntime {
    val modelManager: ModelManager

    /** Per-player media data source (the app's cloud-aware ExoPlayer factory). */
    fun videoDataSourceFactory(): DataSource.Factory

    /** kotlin-inject assisted factory for the per-media keyed player ViewModel. */
    fun videoPlayerViewModel(
        savedStateHandle: SavedStateHandle,
        media: Media,
        dataSourceFactory: DataSource.Factory,
    ): VideoPlayerViewModel

    /**
     * Ensures the cloud original is cached locally and returns a file-backed subsampling
     * [ImageSource] for ZoomImage tiling. Throws on failure — callers catch and trace.
     */
    suspend fun cloudImageSource(
        providerType: ProviderType,
        remoteId: String,
        configId: Long,
        fileId: String? = null,
    ): ImageSource
}

val LocalViewerRuntime = staticCompositionLocalOf<ViewerRuntime?> { null }
