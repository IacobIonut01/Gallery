/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
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
}.onEach { if (!it) delay(1000) }.conflate()