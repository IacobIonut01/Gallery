/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.domain.model.Media
import com.dot.gallery.feature.viewer.ViewerRuntime
import com.dot.gallery.feature.viewer.components.video.VideoPlayerViewModel
import com.github.panpf.zoomimage.subsampling.ImageSource

/**
 * Docs [ViewerRuntime]: real [ModelManager], a local-file [DefaultDataSource.Factory],
 * and genuinely constructed [VideoPlayerViewModel]s — so the captured viewer decodes
 * the same way the app does. Cloud fetch is unsupported: docs scenes never carry
 * cloud media, and silently returning an empty source would photograph as a black
 * frame, so fail loudly instead.
 */
class DocsViewerRuntime(context: Context) : ViewerRuntime {

    private val appContext = context.applicationContext

    override val modelManager: ModelManager = ModelManager(appContext)

    private val localDataSourceFactory = DefaultDataSource.Factory(appContext)

    override fun videoDataSourceFactory(): DataSource.Factory = localDataSourceFactory

    override fun videoPlayerViewModel(
        savedStateHandle: SavedStateHandle,
        media: Media,
        dataSourceFactory: DataSource.Factory,
    ): VideoPlayerViewModel =
        VideoPlayerViewModel(appContext, savedStateHandle, media, dataSourceFactory)

    override suspend fun cloudImageSource(
        providerType: ProviderType,
        remoteId: String,
        configId: Long,
        fileId: String?,
    ): ImageSource = error("cloud image sources are unsupported in docs scenes")
}
