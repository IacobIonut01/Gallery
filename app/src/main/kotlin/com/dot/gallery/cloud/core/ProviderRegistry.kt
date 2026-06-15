/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import com.dot.gallery.cloud.core.capabilities.MapCapableProvider
import com.dot.gallery.cloud.core.capabilities.MemoriesCapableProvider
import com.dot.gallery.cloud.core.capabilities.OcrCapableProvider
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.ShareLinkCapableProvider
import com.dot.gallery.cloud.core.capabilities.SmartSearchCapableProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.feature_node.presentation.util.printDebug
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRegistry @Inject constructor() {

    @PublishedApi
    internal val _providers = mutableMapOf<ProviderType, MediaCapabilityProvider>()

    fun register(provider: MediaCapabilityProvider) {
        _providers[provider.providerType] = provider
        printDebug("ProviderRegistry: Registered ${provider.providerType.displayName} " +
                "with capabilities: ${provider.capabilities}")
    }

    fun unregister(type: ProviderType) {
        _providers.remove(type)
    }

    fun getAll(): List<MediaCapabilityProvider> = _providers.values.toList()

    fun getAvailable(): List<MediaCapabilityProvider> =
        _providers.values.filter { it.isAvailable }

    fun get(type: ProviderType): MediaCapabilityProvider? = _providers[type]

    inline fun <reified T : MediaCapabilityProvider> getByCapability(): List<T> =
        _providers.values.filterIsInstance<T>().filter { it.isAvailable }

    fun hasCapability(cap: ProviderCapability): Boolean =
        _providers.values.any { it.isAvailable && cap in it.capabilities }

    fun hasAnyProvider(): Boolean = _providers.isNotEmpty()

    fun hasConfiguredProvider(): Boolean = _providers.values.any { it.isAvailable }

    fun getRemoteProviders(): List<RemoteMediaProvider> = getByCapability()
    fun getPeopleProviders(): List<PeopleCapableProvider> = getByCapability()
    fun getOcrProviders(): List<OcrCapableProvider> = getByCapability()
    fun getSmartSearchProviders(): List<SmartSearchCapableProvider> = getByCapability()
    fun getMapProviders(): List<MapCapableProvider> = getByCapability()
    fun getShareLinkProviders(): List<ShareLinkCapableProvider> = getByCapability()
    fun getSyncProviders(): List<SyncCapableProvider> = getByCapability()
    fun getMemoriesProviders(): List<MemoriesCapableProvider> = getByCapability()
}
