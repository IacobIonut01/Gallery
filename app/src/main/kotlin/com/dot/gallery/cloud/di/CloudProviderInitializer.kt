/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.di

import com.dot.gallery.cloud.core.CloudRuntimeSettings
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.CredentialEncryptor
import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.ProviderInstanceFactory
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.repository.CloudRepository
import com.dot.gallery.cloud.network.ServerUrlResolver
import com.dot.gallery.cloud.sync.CloudIndexProgressManager
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.presentation.util.printDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
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
    private val providerFactories: Set<@JvmSuppressWildcards ProviderInstanceFactory>,
    private val registry: ProviderRegistry,
    private val configDao: CloudServerConfigDao,
    private val credentialEncryptor: CredentialEncryptor,
    private val cloudMediaDao: CloudMediaDao,
    private val cloudRepository: CloudRepository,
    private val urlResolver: ServerUrlResolver,
    private val indexProgressManager: CloudIndexProgressManager
) {

    private val factoriesByType by lazy { providerFactories.associateBy { it.providerType } }

    /** Last effective (resolved) server URL applied per account, to skip no-op reconfigures. */
    private val lastResolvedUrl = ConcurrentHashMap<Long, String>()

    /** Long-lived scope for non-blocking network prefetches (assets/trash). */
    private val prefetchScope = CoroutineScope(Dispatchers.IO)

    /**
     * App-lifetime scope for account reconfigures. Deliberately NOT tied to any ViewModel/UI
     * scope: a URL switch triggered from a settings screen must survive the user navigating
     * away, otherwise the in-flight re-authentication is cancelled ("Socket closed") and the
     * provider is left half-switched (new base URL, stale/absent auth) with no data reload.
     */
    private val reconfigureScope = CoroutineScope(Dispatchers.IO)

    /**
     * Pages through ALL of [provider]'s remote assets (and its trash) and caches them into
     * Room, non-blocking. This is what makes a provider's media appear in the timeline/albums.
     * Runs for BOTH startup auto-auth and runtime account registration so a freshly added
     * account populates immediately instead of only after the next app start.
     */
    private fun prefetchProviderData(provider: RemoteMediaProvider, label: String, configId: Long) {
        prefetchScope.launch {
            indexProgressManager.start(configId, label)
            try {
                var page = 0
                var total = 0
                while (true) {
                    val resource = provider.getRemoteAssets(page, PREFETCH_PAGE_SIZE).first()
                    if (resource !is Resource.Success) break
                    val items = resource.data ?: emptyList()
                    if (items.isNotEmpty()) {
                        cloudMediaDao.insertAll(items)
                        total += items.size
                        indexProgressManager.update(configId, total, label)
                    }
                    if (items.size < PREFETCH_PAGE_SIZE) break
                    page++
                    if (page >= MAX_PREFETCH_PAGES) break
                }
                printDebug("CloudProviderInitializer: Cached $total assets for $label")
            } catch (e: Exception) {
                printDebug("CloudProviderInitializer: Asset prefetch failed for $label: ${e.message}")
            } finally {
                indexProgressManager.finish(configId)
            }
        }
        prefetchScope.launch {
            try {
                val trashed = provider.getRemoteTrashed().first()
                if (trashed is Resource.Success) {
                    trashed.data?.let { cloudMediaDao.insertAll(it) }
                }
            } catch (e: Exception) {
                printDebug("CloudProviderInitializer: Trash prefetch failed for $label: ${e.message}")
            }
        }
    }

    /**
     * Creates a fresh, UNconfigured provider instance for [type] (or null if that provider
     * is not built into this variant). Used by the add-account wizard to test a connection or
     * list remote albums before the config has been persisted/registered.
     */
    fun createTransientProvider(type: ProviderType): MediaCapabilityProvider? =
        factoriesByType[type]?.create()

    /**
     * Mints (or reuses), configures, authenticates and registers the provider instance for a
     * single account [configId]. Call after a new account is saved so it becomes usable
     * immediately, without waiting for the next app start. Must run off the main thread.
     */
    suspend fun registerAccount(configId: Long): Result<Unit> {
        val entity = configDao.getById(configId)
            ?: return Result.failure(IllegalArgumentException("Cloud account not found"))
        if (!entity.isActive) return Result.failure(IllegalStateException("Cloud account is inactive"))
        val provider = (registry.getByConfigId(configId) as? RemoteMediaProvider)
            ?: (factoriesByType[entity.providerType]?.create() as? RemoteMediaProvider)
            ?: return Result.failure(IllegalStateException("Cloud provider is unavailable"))
        registry.register(entity.id, provider)
        registry.updateConnectionState(entity.id, ConnectionState.AUTHENTICATING)
        return try {
            val config = entity.toCloudServerConfig().let { cfg ->
                cfg.copy(
                    apiKey = cfg.apiKey?.let { credentialEncryptor.decrypt(it) },
                    password = cfg.password?.let { credentialEncryptor.decrypt(it) }
                )
            }
            val resolved = urlResolver.resolve(config)
            provider.configure(resolved)
            provider.authenticate(resolved).getOrThrow()
            lastResolvedUrl[entity.id] = resolved.serverUrl
            registry.updateConnectionState(entity.id, ConnectionState.CONNECTED)
            configDao.updateLastConnected(entity.id, System.currentTimeMillis())
            cloudRepository.notifyProviderConnected(entity.providerType, ConnectionState.CONNECTED)
            prefetchProviderData(provider, entity.displayName.ifBlank { entity.providerType.displayName }, entity.id)
            printDebug("CloudProviderInitializer: Registered account ${entity.providerType} #${entity.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            registry.updateConnectionState(entity.id, ConnectionState.ERROR)
            printDebug("CloudProviderInitializer: registerAccount failed for #${entity.id}: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Auto-configure and authenticate remote providers that have an active
     * server config stored in the database. Must be called from a background
     * coroutine — never from the main thread.
     */
    suspend fun initializeAsync() {
        val activeConfigs = configDao.getAll().first().filter { it.isActive }
        // Prime the global viewer/advanced preferences snapshot from the active account so
        // settings like "Verbose logging" take effect from app start, not only after the
        // user visits the settings screen.
        CloudRuntimeSettings.apply(activeConfigs.firstOrNull()?.toCloudServerConfig())
        for (entity in activeConfigs) {
            val factory = factoriesByType[entity.providerType] ?: continue
            val provider = factory.create() as? RemoteMediaProvider ?: continue
            registry.register(entity.id, provider)
            registry.updateConnectionState(entity.id, ConnectionState.AUTHENTICATING)
            try {
                val config = entity.toCloudServerConfig().let { cfg ->
                    cfg.copy(
                        apiKey = cfg.apiKey?.let { credentialEncryptor.decrypt(it) },
                        password = cfg.password?.let { credentialEncryptor.decrypt(it) }
                    )
                }
                val resolved = urlResolver.resolve(config)
                provider.configure(resolved)
                provider.authenticate(resolved).getOrThrow()
                lastResolvedUrl[entity.id] = resolved.serverUrl
                registry.updateConnectionState(entity.id, ConnectionState.CONNECTED)
                configDao.updateLastConnected(entity.id, System.currentTimeMillis())
                cloudRepository.notifyProviderConnected(entity.providerType, ConnectionState.CONNECTED)
                printDebug("CloudProviderInitializer: Auto-authenticated ${entity.providerType} #${entity.id} with ${resolved.serverUrl}")
                // Proactive cache: fetch fresh data from network in parallel (non-blocking).
                prefetchProviderData(provider, entity.displayName.ifBlank { entity.providerType.displayName }, entity.id)
            } catch (e: Exception) {
                registry.updateConnectionState(entity.id, ConnectionState.ERROR)
                printDebug("CloudProviderInitializer: Auto-auth failed for ${entity.providerType} #${entity.id}: ${e.message}")
            }
        }
    }

    /**
     * Re-resolve and re-apply the server URL for a single account [configId] after its config
     * changed — e.g. the user toggled automatic URL switching or edited the local URL/SSID in
     * settings. Unlike [reconfigureActiveProviders] this does NOT require [autoUrlSwitch] to be
     * enabled, so turning the feature OFF correctly reverts the provider to its external URL.
     * No-op when the effective URL is unchanged. Must run off the main thread.
     */
    fun reconfigureAccountAsync(configId: Long) {
        reconfigureScope.launch { reconfigureAccount(configId) }
    }

    suspend fun reconfigureAccount(configId: Long) {
        val entity = configDao.getById(configId) ?: return
        if (!entity.isActive) return
        val provider = registry.getByConfigId(configId) as? RemoteMediaProvider ?: return
        try {
            val config = entity.toCloudServerConfig().let { cfg ->
                cfg.copy(
                    apiKey = cfg.apiKey?.let { credentialEncryptor.decrypt(it) },
                    password = cfg.password?.let { credentialEncryptor.decrypt(it) }
                )
            }
            val resolved = urlResolver.resolve(config)
            if (lastResolvedUrl[entity.id] == resolved.serverUrl) return
            registry.updateConnectionState(entity.id, ConnectionState.AUTHENTICATING)
            provider.configure(resolved)
            provider.authenticate(resolved).getOrThrow()
            lastResolvedUrl[entity.id] = resolved.serverUrl
            registry.updateConnectionState(entity.id, ConnectionState.CONNECTED)
            configDao.updateLastConnected(entity.id, System.currentTimeMillis())
            cloudRepository.notifyProviderConnected(entity.providerType, ConnectionState.CONNECTED)
            printDebug("CloudProviderInitializer: Reconfigured account #${entity.id} -> ${resolved.serverUrl}")
            // Re-pull data from the new URL so the timeline/albums reflect the switched host.
            prefetchProviderData(provider, entity.displayName.ifBlank { entity.providerType.displayName }, entity.id)
        } catch (e: Exception) {
            registry.updateConnectionState(entity.id, ConnectionState.ERROR)
            printDebug("CloudProviderInitializer: reconfigureAccount failed for #${entity.id}: ${e.message}")
        }
    }

    /**
     * Re-resolve and re-apply server URLs for active auto-URL-switching providers. Intended to
     * be called when the network changes (e.g. moving between the local network and mobile data).
     * Only providers whose effective URL actually changed are reconfigured + re-authenticated.
     * Safe to call repeatedly; must run off the main thread.
     */
    suspend fun reconfigureActiveProviders() {
        val activeConfigs = configDao.getAll().first().filter { it.isActive && it.autoUrlSwitch }
        for (entity in activeConfigs) {
            val provider = registry.getByConfigId(entity.id) as? RemoteMediaProvider ?: continue
            try {
                val config = entity.toCloudServerConfig().let { cfg ->
                    cfg.copy(
                        apiKey = cfg.apiKey?.let { credentialEncryptor.decrypt(it) },
                        password = cfg.password?.let { credentialEncryptor.decrypt(it) }
                    )
                }
                val resolved = urlResolver.resolve(config)
                if (lastResolvedUrl[entity.id] == resolved.serverUrl) continue
                registry.updateConnectionState(entity.id, ConnectionState.AUTHENTICATING)
                provider.configure(resolved)
                provider.authenticate(resolved).getOrThrow()
                lastResolvedUrl[entity.id] = resolved.serverUrl
                registry.updateConnectionState(entity.id, ConnectionState.CONNECTED)
                configDao.updateLastConnected(entity.id, System.currentTimeMillis())
                cloudRepository.notifyProviderConnected(entity.providerType, ConnectionState.CONNECTED)
                printDebug("CloudProviderInitializer: Reconfigured ${entity.providerType} #${entity.id} -> ${resolved.serverUrl}")
            } catch (e: Exception) {
                registry.updateConnectionState(entity.id, ConnectionState.ERROR)
                printDebug("CloudProviderInitializer: Reconfigure failed for ${entity.providerType} #${entity.id}: ${e.message}")
            }
        }
    }

    companion object {
        /** Page size for the startup asset prefetch. */
        private const val PREFETCH_PAGE_SIZE = 200

        /**
         * Hard cap on prefetch pages (safety valve against a misbehaving provider that never
         * returns a short page). 500 pages * 200 = 100k assets, well beyond typical libraries.
         */
        private const val MAX_PREFETCH_PAGES = 500
    }
}
