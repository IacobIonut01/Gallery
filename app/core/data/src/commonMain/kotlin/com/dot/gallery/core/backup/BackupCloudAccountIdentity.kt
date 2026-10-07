/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

data class BackupCloudAccountIdentity(
    val providerType: String,
    val serverUrl: String,
    val username: String
)

fun backupCloudAccountIdentity(
    providerType: String,
    serverUrl: String,
    username: String?
): BackupCloudAccountIdentity = BackupCloudAccountIdentity(
    providerType = providerType,
    serverUrl = serverUrl.trim().trimEnd('/'),
    username = username.orEmpty()
)
