/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.di

import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.CredentialEncryptor
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.presentation.util.printDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Eagerly initializes all cloud providers by accepting the multibinding set.
 * Each provider module contributes to the set via @IntoSet, and
 * the provider's @Provides method handles registration into ProviderRegistry.
 *
 * Inject this class in GalleryApp.onCreate() to trigger initialization.
 * Call [initializeAsync] from a background coroutine to auto-configure
 * providers that have stored server configs.
 */
@Singleton
class CloudProviderInitializer @Inject constructor(
    val providers: Set<@JvmSuppressWildcards MediaCapabilityProvider>,
    private val configDao: CloudServerConfigDao,
    private val credentialEncryptor: CredentialEncryptor,
    private val cloudMediaDao: CloudMediaDao,
    private val cloudRepository: CloudRepository
) {

    /**
     * Auto-configure and authenticate remote providers that have an active
     * server config stored in the database. Must be called from a background
     * coroutine — never from the main thread.
     */
    suspend fun initializeAsync() {
        val prefetchScope = CoroutineScope(Dispatchers.IO)
        for (provider in providers) {
            if (provider is RemoteMediaProvider) {
                try {
                    val entity = configDao.getActiveByProvider(provider.providerType)
                        ?: continue
                    val config = entity.toCloudServerConfig().let { cfg ->
                        cfg.copy(
                            apiKey = cfg.apiKey?.let { credentialEncryptor.decrypt(it) },
                            password = cfg.password?.let { credentialEncryptor.decrypt(it) }
                        )
                    }
                    provider.configure(config)
                    provider.authenticate(config)
                    // Notify CONNECTED immediately so cached data from Room is displayed right away
                    cloudRepository.notifyProviderConnected(provider.providerType, ConnectionState.CONNECTED)
                    printDebug("CloudProviderInitializer: Auto-authenticated ${provider.providerType} with ${config.serverUrl}")
                    // Proactive cache: fetch fresh data from network in parallel (non-blocking)
                    prefetchScope.launch {
                        try {
                            val resource = provider.getRemoteAssets(0, 200).first()
                            if (resource is Resource.Success) {
                                resource.data?.let { cloudMediaDao.insertAll(it) }
                                printDebug("CloudProviderInitializer: Cached ${resource.data?.size ?: 0} assets for ${provider.providerType}")
                            }
                        } catch (e: Exception) {
                            printDebug("CloudProviderInitializer: Asset prefetch failed for ${provider.providerType}: ${e.message}")
                        }
                    }
                    prefetchScope.launch {
                        try {
                            val trashed = provider.getRemoteTrashed().first()
                            if (trashed is Resource.Success) {
                                trashed.data?.let { cloudMediaDao.insertAll(it) }
                                printDebug("CloudProviderInitializer: Cached ${trashed.data?.size ?: 0} trashed assets for ${provider.providerType}")
                            }
                        } catch (e: Exception) {
                            printDebug("CloudProviderInitializer: Trash prefetch failed for ${provider.providerType}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    printDebug("CloudProviderInitializer: Auto-auth failed for ${provider.providerType}: ${e.message}")
                }
            }
        }
    }
}
