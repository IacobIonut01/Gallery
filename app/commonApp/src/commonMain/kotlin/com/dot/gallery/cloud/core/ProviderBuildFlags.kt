/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.BuildConfig

val ProviderType.isIncludedInBuild: Boolean
    get() = when (this) {
        ProviderType.IMMICH -> BuildConfig.IMMICH_ENABLED
        ProviderType.PHOTOPRISM -> BuildConfig.PHOTOPRISM_ENABLED
        ProviderType.OWNCLOUD -> BuildConfig.OWNCLOUD_ENABLED
        ProviderType.NEXTCLOUD -> BuildConfig.NEXTCLOUD_ENABLED
        ProviderType.WEBDAV -> BuildConfig.WEBDAV_ENABLED
        ProviderType.SMB -> BuildConfig.SMB_ENABLED
        ProviderType.NFS -> BuildConfig.NFS_ENABLED
        else -> true
    }

fun ProviderType.Companion.availableRemoteTypes(): List<ProviderType> =
    ProviderType.entries.filter { it.isRemote && it.isIncludedInBuild }

fun ProviderType.Companion.hasAnyRemoteProvider(): Boolean =
    ProviderType.entries.any { it.isRemote && it.isIncludedInBuild }
