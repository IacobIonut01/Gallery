/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import com.dot.gallery.core.Settings.Misc.getStoredMediaVersion
import com.dot.gallery.core.Settings.Misc.updateStoredMediaVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Register an observer class that gets callbacks when data identified by a given content URI
 * changes.
 */
fun Context.contentFlowObserver(uris: Array<Uri>) = callbackFlow {
    val ctx = this@contentFlowObserver
    val mutex = Mutex()
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (isActive) {
                launch(Dispatchers.IO) {
                    mutex.withLock {
                        val mediaStoreVersion = MediaStore.getVersion(ctx)
                        if (getStoredMediaVersion(ctx) != mediaStoreVersion) {
                            updateStoredMediaVersion(ctx, mediaStoreVersion)
                            trySend(selfChange)
                        }
                    }
                }
            }
        }
    }
    for (uri in uris)
        contentResolver.registerContentObserver(uri, true, observer)
    // trigger first.
    launch(Dispatchers.IO) {
        mutex.withLock {
            updateStoredMediaVersion(ctx, MediaStore.getVersion(ctx))
            trySend(false)
        }
    }
    awaitClose {
        contentResolver.unregisterContentObserver(observer)
    }
}