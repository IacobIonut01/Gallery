/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.mediaview

import android.content.Context
import android.content.Intent
import com.dot.gallery.core.ui.mediaview.KnownVisualSearchProvider
import com.dot.gallery.core.ui.mediaview.VisualSearchTarget
import com.dot.gallery.BuildConfig

/**
 * Discovers installed activities that accept image shares. The model/catalog types live in
 * `com.dot.gallery.core.ui.mediaview` (:core:ui); this stays app-side because it needs
 * `PackageManager` access.
 */
fun Context.discoverVisualSearchTargets(): List<VisualSearchTarget> {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "image/*" }
    return packageManager.queryIntentActivities(intent, 0)
        .filter { it.activityInfo != null }
        .filterNot { it.activityInfo.packageName == BuildConfig.APPLICATION_ID }
        .distinctBy { it.activityInfo.packageName }
        .map { resolveInfo ->
            VisualSearchTarget(
                packageName = resolveInfo.activityInfo.packageName,
                activityName = resolveInfo.activityInfo.name,
                label = resolveInfo.loadLabel(packageManager).toString(),
                known = KnownVisualSearchProvider.forPackage(resolveInfo.activityInfo.packageName),
            )
        }
        .sortedWith(
            compareBy(
                // Lens leads, then the rest of the catalog in declaration order, then A–Z.
                { it.known != KnownVisualSearchProvider.GOOGLE_LENS },
                { it.known?.ordinal ?: Int.MAX_VALUE },
                { it.label.lowercase() },
            )
        )
}
