/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Register an observer class that gets callbacks when data identified by a given content URI
 * changes.
 */
fun ContentResolver.contentFlowObserver(uris: Array<Uri>) = callbackFlow {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (isActive) {
                launch(Dispatchers.IO) {
                    send(selfChange)
                }
            }
        }
    }
    for (uri in uris)
        registerContentObserver(uri, true, observer)
    // trigger first.
    trySend(false)
    awaitClose {
        unregisterContentObserver(observer)
    }
}.conflate()