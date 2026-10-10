/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.offline

/** Every operation [OfflineModeContent] delegates to its host ViewModel. */
data class OfflineModeContentActions(
    val setForceOffline: (Boolean) -> Unit = {},
    val setCacheOnView: (Boolean) -> Unit = {},
    val setCacheWifiOnly: (Boolean) -> Unit = {},
    val setBudgetMb: (Int) -> Unit = {},
    val pinAccount: (Long, String) -> Unit = { _, _ -> },
    val unpinAccount: (Long, Boolean) -> Unit = { _, _ -> },
    val downloadNow: () -> Unit = {},
    val openAccountCache: (Long, String) -> Unit = { _, _ -> },
    val closeAccountCache: () -> Unit = {},
    val clearAccountCache: (Long) -> Unit = {},
    val clearAlbumCache: (Long, String) -> Unit = { _, _ -> },
    val clearAllCache: () -> Unit = {},
    val clearAutoCache: () -> Unit = {},
)
