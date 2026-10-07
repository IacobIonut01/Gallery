/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.ui

import com.dot.gallery.cloud.core.ProviderCapability


/**
 * Which cloud-powered Library tiles/sections a provider configuration exposes —
 * computed from capabilities before any data flows, kept separate from
 * [CloudLibraryState] so the screen can merge availability with live counts.
 */
data class CloudLibraryAvailability(
    val hasCloud: Boolean,
    val isConnected: Boolean,
    val hasArchive: Boolean,
    val hasMemories: Boolean,
    val hasShareLink: Boolean,
    val hasPeople: Boolean,
    val hasMap: Boolean,
)

fun resolveCloudLibraryAvailability(
    hasConfiguredAccounts: Boolean,
    configuredCapabilities: Set<ProviderCapability>,
    isConnected: Boolean,
): CloudLibraryAvailability = CloudLibraryAvailability(
    hasCloud = hasConfiguredAccounts,
    isConnected = isConnected,
    hasArchive = ProviderCapability.ARCHIVE in configuredCapabilities,
    hasMemories = ProviderCapability.MEMORIES in configuredCapabilities,
    hasShareLink = ProviderCapability.SHARE_MANAGE in configuredCapabilities,
    hasPeople = ProviderCapability.PEOPLE in configuredCapabilities,
    hasMap = ProviderCapability.MAP in configuredCapabilities,
)
