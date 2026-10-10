/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.settings

import com.dot.gallery.cloud.core.CloudStorageInfo
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.ui.CloudAccountsViewModel
import com.dot.gallery.data.dao.PendingDownloadInfo
import com.dot.gallery.data.entity.CloudServerConfigEntity

/**
 * Everything [CloudProviderSettingsContent] draws. The screen wrapper collects
 * the real flows and fills this; the docs capture tier supplies a sample
 * account instead.
 */
data class CloudProviderSettingsContentState(
    val config: CloudServerConfigEntity,
    val connectionState: ConnectionState,
    val storage: CloudStorageInfo? = null,
    val serverVersion: String? = null,
    val syncProgress: CloudAccountsViewModel.SyncProgress? = null,
    val copiesInfo: PendingDownloadInfo? = null,
    val removingCopies: Boolean = false,
    val deleting: Boolean = false,
    val deletionFailed: Boolean = false,
)
