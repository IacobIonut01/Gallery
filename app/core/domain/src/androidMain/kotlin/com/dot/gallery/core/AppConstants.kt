/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

object AppConstants {

    /**
     * Default logging tag
     */
    const val TAG = "DotGallery"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val PERMISSION_T = buildList {
        add(Manifest.permission.READ_MEDIA_IMAGES)
        add(Manifest.permission.READ_MEDIA_VIDEO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
    }

    private val PERMISSION_Q = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val PERMISSION_OLD = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    /**
     * Permissions requested for core media access. ACCESS_MEDIA_LOCATION is intentionally
     * separate and optional: denying embedded GPS access must not block the gallery.
     */
    val PERMISSIONS = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> PERMISSION_T
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> PERMISSION_Q
        else -> PERMISSION_OLD
    }
}
