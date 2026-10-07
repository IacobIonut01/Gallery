/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.MediaStore

val Context.mediaStoreVersion: String
    @SuppressLint("NewApi")
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val generations = MediaStore.getExternalVolumeNames(this)
            .sorted()
            .joinToString(separator = ",") { volume ->
                "$volume:${MediaStore.getGeneration(this, volume)}"
            }
        "$generations/${MediaStore.getVersion(this)}"
    } else {
        MediaStore.getVersion(this)
    }
