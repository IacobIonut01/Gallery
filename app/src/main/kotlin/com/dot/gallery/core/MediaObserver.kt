/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.content.Context
import android.os.FileObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

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
}.conflate()
