/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import android.content.Context
import com.dot.gallery.cloud.core.CloudAlbumIdentity
import com.dot.gallery.cloud.core.capabilities.RemoteNameConflictPolicy
import com.dot.gallery.domain.model.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.cloud.core.matches
import com.dot.gallery.core.logging.printWarn


@Serializable
enum class CloudAlbumTransferMode {
    COPY,
    MOVE
}