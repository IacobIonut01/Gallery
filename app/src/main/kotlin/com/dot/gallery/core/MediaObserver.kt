/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.FileObserver
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private var observerJob: Job? = null
/**
 * Register an observer class that gets callbacks when data identified by a given content URI
 * changes.
 */
fun Context.contentFlowObserver(uris: Array<Uri>) = callbackFlow {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            observerJob?.cancel()
            observerJob = launch(Dispatchers.IO) {
                send(false)
            }
        }
    }
    for (uri in uris)
        contentResolver.registerContentObserver(uri, true, observer)
    // trigger first.
    observerJob = launch(Dispatchers.IO) {
        send(true)
    }
    awaitClose {
        contentResolver.unregisterContentObserver(observer)
    }
}.conflate().onEach { if (!it) delay(1000) }

fun Context.contentFlowWithDatabase(uris: Array<Uri>, database: InternalDatabase) = merge(
    contentFlowObserver(uris),
    database.getBlacklistDao().getBlacklistedAlbums()
)


private var observerFileJob: Job? = null
fun Context.fileFlowObserver() = callbackFlow {
    val observer = object : FileObserver(filesDir, CREATE or DELETE or MODIFY or MOVED_FROM or MOVED_TO) {
        override fun onEvent(event: Int, path: String?) {
            observerFileJob?.cancel()
            observerFileJob = launch(Dispatchers.IO) {
                send(true)
            }
        }
    }
    observer.startWatching()
    observerFileJob = launch(Dispatchers.IO) {
        send(true)
    }
    awaitClose {
        observer.stopWatching()
    }
}