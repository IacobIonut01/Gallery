/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ml

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.dot.gallery.core.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.core.logging.printInfo
import com.dot.gallery.core.logging.printWarning
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.core.AppIdentity

class ModelsNotAvailableException(
    message: String = "ML models are not installed. Download them from Settings > Smart Features."
) : RuntimeException(message)
