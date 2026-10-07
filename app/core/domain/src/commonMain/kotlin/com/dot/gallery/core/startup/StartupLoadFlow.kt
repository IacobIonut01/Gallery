/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import com.dot.gallery.core.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow


fun <T> startupLoadFlow(
    readCache: suspend () -> List<T>?,
    boundedSource: (suspend () -> List<T>)?,
    awaitFirstContent: suspend () -> Unit,
    currentStamp: suspend () -> StartupCacheStamp?,
    liveSource: Flow<List<T>>,
    writeCache: suspend (StartupCacheStamp?, List<T>) -> Unit,
    onLiveError: (Throwable) -> Unit,
    errorMessage: String = "Failed to load media"
): Flow<Resource<List<T>>> = flow {
    var emitted = false
    emitAll(flow<Resource<List<T>>> {
        val cached = try {
            readCache()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            onLiveError(e)
            null
        }
        val initial = cached ?: boundedSource?.invoke()
        if (initial != null) {
            emit(Resource.Success(initial, isPartial = true))
            emitted = true
            awaitFirstContent()
        }
        val stamp = currentStamp()
        liveSource.collect { full ->
            emit(Resource.Success(full))
            emitted = true
            try {
                writeCache(stamp, full)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onLiveError(e)
            }
        }
    }.catch { e ->
        if (e is CancellationException) throw e
        onLiveError(e)
        if (!emitted) emit(Resource.Error(errorMessage))
    })
}
