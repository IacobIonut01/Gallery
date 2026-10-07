/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.logging

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.text.format.Formatter
import com.dot.gallery.core.util.or
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.domain.uri.path
import com.dot.gallery.core.AppIdentity


/**
 * Anonymized device/app snapshot prepended to every exported bundle and bug
 * report — app version, build flavor, OS, device, ABI, free storage. Never
 * includes locale, accounts, or any user identifier.
 */
object DiagnosticInfo {

    fun snapshot(context: Context): List<Pair<String, String>> {
        val freeBytes = runCatching { StatFs(context.filesDir.path).availableBytes }.getOrNull()
        return buildList {
            add("App" to "${AppIdentity.versionName} (${AppIdentity.versionCode})")
            add("Build" to listOfNotNull(
                AppIdentity.flavor.takeIf { it.isNotBlank() },
                AppIdentity.buildType
            ).joinToString("-"))
            add("Device" to "${Build.MANUFACTURER} ${Build.MODEL}")
            add("Android" to "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            add("ABI" to Build.SUPPORTED_ABIS.joinToString(", "))
            if (freeBytes != null) {
                add("Free storage" to Formatter.formatShortFileSize(context, freeBytes))
            }
        }
    }

    fun asText(context: Context): String =
        snapshot(context).joinToString("\n") { (k, v) -> "$k: $v" }
}
