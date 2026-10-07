/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

import com.dot.gallery.cloud.core.CloudAccountRuntimeSettings
import com.dot.gallery.cloud.core.CloudRuntimeSettings
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.feature.viewer.MediaActionCapabilities
import com.dot.gallery.feature.viewer.viewerActionCapabilities
import com.dot.gallery.domain.util.isCloud
import com.dot.gallery.domain.util.canMakeActions


fun cloudMediaReadOnly(
    uriString: String,
    settingsByConfigId: Map<Long, CloudAccountRuntimeSettings>,
): Boolean = CloudRuntimeSettings
    .settingsForCloudUri(uriString, settingsByConfigId)
    .readOnlyMode

fun Media.isReadOnlyCloudMedia(
    settingsByConfigId: Map<Long, CloudAccountRuntimeSettings>,
): Boolean = isCloud && cloudMediaReadOnly(getUri().toString(), settingsByConfigId)

fun Media.viewerActionCapabilities(
    settingsByConfigId: Map<Long, CloudAccountRuntimeSettings>,
    providerSupportsFavorite: Boolean = false,
    providerSupportsTrash: Boolean = false,
    platformSupportsFavorite: Boolean = false,
    sourceAllowsDelete: Boolean = canMakeActions,
    vaultRestoreAvailable: Boolean = false,
    vaultDeleteAvailable: Boolean = false,
): MediaActionCapabilities = viewerActionCapabilities(
    cloudReadOnly = isReadOnlyCloudMedia(settingsByConfigId),
    providerSupportsFavorite = providerSupportsFavorite,
    providerSupportsTrash = providerSupportsTrash,
    platformSupportsFavorite = platformSupportsFavorite,
    sourceAllowsDelete = sourceAllowsDelete,
    vaultRestoreAvailable = vaultRestoreAvailable,
    vaultDeleteAvailable = vaultDeleteAvailable,
)
