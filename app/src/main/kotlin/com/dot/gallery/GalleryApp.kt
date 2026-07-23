/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import android.annotation.SuppressLint
import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.di.CloudProviderInitializer
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import com.dot.gallery.cloud.image.supportCloudMedia
import com.dot.gallery.cloud.network.LanBindingSocketFactory
import com.dot.gallery.cloud.offline.CloudCacheInterceptor
import com.dot.gallery.cloud.offline.CloudMediaCache
import com.dot.gallery.cloud.offline.OfflineModeManager
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.core.metadata.MetadataSanitizer
import com.dot.gallery.core.sandbox.IsolatedImageDecoder
import com.dot.gallery.core.sandbox.SandboxedDecoderHolder
import com.dot.gallery.core.security.AdvancedProtectionMonitor
import com.dot.gallery.core.decoder.supportApng
import com.dot.gallery.core.decoder.supportHeifDecoder
import com.dot.gallery.core.decoder.supportAnimatedJxlDecoder
import com.dot.gallery.core.decoder.supportJxlDecoder
import com.dot.gallery.core.decoder.supportSandboxedHeifDecoder
import com.dot.gallery.core.decoder.supportSandboxedJxlDecoder
import com.dot.gallery.core.decoder.supportVaultDecoder
import com.dot.gallery.core.decoder.supportVideoFrame2
import com.dot.gallery.core.decoder.supportPsdDecoder
import com.dot.gallery.core.decoder.supportJp2Decoder
import com.dot.gallery.core.decoder.supportTiffDecoder
import com.dot.gallery.core.decoder.supportRawDecoder
import com.dot.gallery.core.workers.MetadataCollectionWorker
import com.dot.gallery.core.workers.TempVaultCleanupWorker
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.cache.MemoryCache
import com.github.panpf.sketch.decode.supportAnimatedHeif
import com.github.panpf.sketch.decode.supportAnimatedWebp
import com.github.panpf.sketch.decode.supportGif
import com.github.panpf.sketch.decode.supportSvg
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.saveCellularTraffic
import com.github.panpf.sketch.request.supportPauseLoadWhenScrolling
import com.github.panpf.sketch.resize.Precision
import com.github.panpf.sketch.util.appCacheDirectory
import dagger.hilt.android.HiltAndroidApp
import okio.FileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.dot.gallery.core.metrics.StartupTracer
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** Reserved [com.dot.gallery.cloud.core.ProviderRegistry] config id for the local People provider. */
private const val LOCAL_PEOPLE_CONFIG_ID = -1000L

@HiltAndroidApp
class GalleryApp : Application(), SingletonSketch.Factory, Configuration.Provider {

    @SuppressLint("NewApi")
    override fun createSketch(context: PlatformContext): Sketch = StartupTracer.trace("Sketch.create") { Sketch.Builder(this).apply {
        components {
            supportPauseLoadWhenScrolling()
            supportSvg()
            supportGif()
            supportApng()
            supportVideoFrame2()
            supportAnimatedWebp()
            supportAnimatedHeif()
            supportSandboxedHeifDecoder()
            supportSandboxedJxlDecoder()
            supportAnimatedJxlDecoder()
            supportHeifDecoder()
            supportJxlDecoder()
            supportPsdDecoder()
            supportJp2Decoder()
            supportTiffDecoder()
            supportRawDecoder()
            supportVaultDecoder()
            supportCloudMedia()
        }
        val diskCache = DiskCache.Builder(context, FileSystem.SYSTEM)
            .directory(context.appCacheDirectory())
            .maxSize(150 * 1024 * 1024).build()

        memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.70)
                .build()
        }

        decodeParallelismLimited(maxOf(2, Runtime.getRuntime().availableProcessors().coerceAtMost(6)))

        resultCache(diskCache)
        downloadCache(diskCache)

        globalImageOptions(
            ImageOptions {
                crossfade(false)
                precision(Precision.LESS_PIXELS)
                saveCellularTraffic(false)
            }
        )
    }.build() }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var repository: MediaRepository

    @Inject
    lateinit var metadataSanitizer: MetadataSanitizer

    @Inject
    lateinit var mediaDistributor: MediaDistributor

    @Inject
    lateinit var modelManager: ModelManager

    @Inject
    lateinit var isolatedImageDecoder: IsolatedImageDecoder

    @Inject
    lateinit var providerRegistry: ProviderRegistry

    @Inject
    lateinit var cloudProviderInitializer: CloudProviderInitializer

    @Inject
    lateinit var localPeopleProvider: com.dot.gallery.cloud.local.LocalPeopleProvider

    @Inject
    lateinit var cloudMediaCache: CloudMediaCache

    @Inject
    lateinit var offlineModeManager: OfflineModeManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        val onCreateSpan = StartupTracer.begin("App.onCreate")
        // Isolated-process services inherit this Application class but must NOT
        // run Hilt injection (WorkManager/JobScheduler are unavailable there).
        if (getProcessName() != packageName) return

        StartupTracer.trace("App.super.onCreate (Hilt DI)") {
            super.onCreate()
        }

        appScope.launch {
            metadataSanitizer.recoverPendingTransactions()
        }

        CloudFetcherRegistryHolder.registry = providerRegistry
        // Register on-device local capability providers (People). Keyed by a reserved negative
        // config id so it never collides with real cloud account ids. isAvailable() gates on the
        // face model being installed, so it stays hidden until models are present.
        providerRegistry.register(LOCAL_PEOPLE_CONFIG_ID, localPeopleProvider)
        // ONE shared OkHttp client for all cloud image/video loading (Glide, Sketch, ZoomImage,
        // ExoPlayer). A single pooled client keeps connections warm — a big win for a flinging
        // grid hitting the same host repeatedly — and a disk cache avoids re-downloading
        // thumbnails across sessions. Previously a client was only created on insecure-TLS builds
        // (and with no cache), so secure builds fell back to a fresh, uncached OkHttpClient() per
        // request.
        run {
            val builder = OkHttpClient.Builder()
                .cache(Cache(File(cacheDir, "cloud_http_cache"), 256L * 1024 * 1024))
                // Persistent offline cache: serves keyed image requests from disk (even online),
                // fails fast when offline + uncached, and write-through-caches new responses.
                .addInterceptor(CloudCacheInterceptor(cloudMediaCache, offlineModeManager))
                // Bind LAN-destined image/video sockets to Wi-Fi so thumbnails load from a
                // local server even when that Wi-Fi has no internet (default net = mobile data).
                .socketFactory(LanBindingSocketFactory(this))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
            if (BuildConfig.ALLOW_INSECURE_TLS) {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    .hostnameVerifier { _, _ -> true }
            }
            CloudFetcherRegistryHolder.okHttpClient = builder.build()
        }

        StartupTracer.trace("AdvancedProtectionMonitor.init") {
            AdvancedProtectionMonitor.init(this)
        }

        StartupTracer.trace("SandboxedDecoderHolder.init") {
            SandboxedDecoderHolder.init(isolatedImageDecoder, this)
        }

        StartupTracer.trace("WorkManager.enqueueMetadata") {
            workManager.enqueueUniqueWork(
                uniqueWorkName = "MetadataCollection",
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
                request = OneTimeWorkRequestBuilder<MetadataCollectionWorker>()
                    .build()
            )
        }

        StartupTracer.trace("TempVaultCleanupWorker.schedule") {
            TempVaultCleanupWorker.schedule(workManager)
        }

        // One-time cleanup of leaked vault temp files for users upgrading from affected versions
        appScope.launch(Dispatchers.IO) {
            TempVaultCleanupWorker.runLegacyFilesdirCleanup(this@GalleryApp)
        }

        // Initialize ML models (copies from assets on withML, checks presence on noML)
        appScope.launch {
            StartupTracer.trace("ModelManager.initializeModels") {
                modelManager.initializeModels()
            }
        }

        // Auto-configure cloud providers asynchronously (off main thread)
        appScope.launch {
            cloudProviderInitializer.initializeAsync()
        }

        // Re-resolve auto-switching server URLs when the network changes
        // (e.g. moving between the local Wi-Fi and mobile data).
        registerNetworkChangeReconfigure()

        StartupTracer.end(onCreateSpan)
    }

    /**
     * Registers a default-network callback that re-resolves auto-switching server URLs whenever
     * connectivity changes, so providers transparently switch between their local and external
     * URLs when the device joins/leaves the configured local network.
     */
    private fun registerNetworkChangeReconfigure() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                appScope.launch { cloudProviderInitializer.reconfigureActiveProviders() }
            }

            override fun onLost(network: Network) {
                appScope.launch { cloudProviderInitializer.reconfigureActiveProviders() }
            }
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
            // ACCESS_NETWORK_STATE unavailable or callback registration failed; skip auto-switch.
        }
    }

}