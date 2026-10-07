/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import java.io.File
import okio.Path
import okio.Path.Companion.toPath

fun sketchCacheDirectory(internalCacheDirectory: File): Path =
    File(internalCacheDirectory, "sketch").absolutePath.toPath()
