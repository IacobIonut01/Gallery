/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.net.Uri
import com.dot.gallery.core.smart.SmartScanScheduler
import com.dot.gallery.core.work.WorkSignals
import com.dot.gallery.core.work.workDataJson
import com.dot.gallery.core.workers.CategoryWorker
import com.dot.gallery.core.workers.VaultOperationWorker
import com.dot.gallery.core.workers.enqueueVaultOperation
import com.dot.gallery.core.workers.stopClassification
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.TaskState
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.dot.gallery.data.dao.SmartScanScheduleResult
import com.dot.gallery.domain.model.GeoMedia
import com.dot.gallery.domain.model.LocationMedia
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.domain.model.Vault
import com.dot.gallery.feature.classifier.ClassifierRuntime
import com.dot.gallery.feature.library.location.MapGeoMediaSource
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class ClassifierRuntimeImpl @Inject constructor(
    private val scheduler: BackgroundTaskScheduler,
    private val smartScanScheduler: SmartScanScheduler,
    private val mapGeoMediaSource: MapGeoMediaSource,
) : ClassifierRuntime {

    override suspend fun smartScanManual(features: Int): SmartScanScheduleResult =
        smartScanScheduler.manual(features)

    override suspend fun smartScanAutomatic(features: Int): SmartScanScheduleResult =
        smartScanScheduler.automatic(features)

    override suspend fun smartScanCancel(runId: String): Boolean =
        smartScanScheduler.cancel(runId)

    override fun stopClassification() = scheduler.stopClassification()

    override suspend fun enqueueVaultEncrypt(media: List<Uri>, vault: Vault) {
        scheduler.enqueueVaultOperation(
            operation = VaultOperationWorker.OP_ENCRYPT,
            media = media,
            vault = vault
        )
    }

    override val classifierRunning: Flow<Boolean> = flow {
        while (currentCoroutineContext().isActive) {
            val running = scheduler.queryTasks(
                tags = setOf("ImageClassifier"),
                states = setOf(TaskState.Kind.RUNNING)
            ).isNotEmpty()
            emit(running)
            delay(1_000)
        }
    }

    override val classifierProgress: Flow<Float> =
        WorkSignals.progressFlow(CategoryWorker.WORK_NAME)
            .map { it?.get(CategoryWorker.KEY_PROGRESS)?.jsonPrimitive?.floatOrNull ?: 0f }

    override fun mergedGeoMedia(
        localGeoMedia: Flow<List<GeoMedia>>,
        timelineMedia: Flow<MediaState<UriMedia>>,
    ): Flow<List<GeoMedia>> =
        mapGeoMediaSource.mergedGeoMedia(localGeoMedia, timelineMedia)

    override fun mergedLocations(
        localLocations: Flow<List<LocationMedia>>,
        geoMedia: Flow<List<GeoMedia>>,
        timelineMedia: Flow<MediaState<UriMedia>>,
    ): Flow<List<LocationMedia>> =
        mapGeoMediaSource.mergedLocations(localLocations, geoMedia, timelineMedia)
}
