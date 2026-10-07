/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.classifier

import android.net.Uri
import com.dot.gallery.data.dao.SmartScanScheduleResult
import com.dot.gallery.domain.model.GeoMedia
import com.dot.gallery.domain.model.LocationMedia
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.model.Vault
import kotlinx.coroutines.flow.Flow

/**
 * App-provided runtime surface for the classifier ViewModels. The VMs live in this module
 * while the work plumbing (WorkManager workers, [SmartScanScheduler], geo merge source)
 * stays app-side — this interface is the bridge, bound in `AppComponent`.
 */
interface ClassifierRuntime {

    // ---- SmartScanScheduler surface (features = SmartScanFeature bit mask) ----
    suspend fun smartScanManual(features: Int): SmartScanScheduleResult
    suspend fun smartScanAutomatic(features: Int): SmartScanScheduleResult
    suspend fun smartScanCancel(runId: String): Boolean

    /** Cancels the legacy ClassifierWorker unique work. */
    fun stopClassification()

    /** Enqueues a vault ENCRYPT operation for the given media uris. */
    suspend fun enqueueVaultEncrypt(media: List<Uri>, vault: Vault)

    /** Whether the "ImageClassifier"-tagged work is currently running. */
    val classifierRunning: Flow<Boolean>

    /** 0f..1f progress of the "ImageClassifier"-tagged work. */
    val classifierProgress: Flow<Float>

    // ---- MapGeoMediaSource surface ----
    fun mergedGeoMedia(
        localGeoMedia: Flow<List<GeoMedia>>,
        timelineMedia: Flow<MediaState<UriMedia>>,
    ): Flow<List<GeoMedia>>

    fun mergedLocations(
        localLocations: Flow<List<LocationMedia>>,
        geoMedia: Flow<List<GeoMedia>>,
        timelineMedia: Flow<MediaState<UriMedia>>,
    ): Flow<List<LocationMedia>>
}
