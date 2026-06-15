/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery

import android.annotation.SuppressLint
import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.di.CloudProviderInitializer
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import com.dot.gallery.cloud.image.supportCloudMedia
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.core.ml.ModelManager
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
import okhttp3.OkHttpClient
import java.security.SecureRandom
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
    lateinit var mediaDistributor: MediaDistributor

    @Inject
    lateinit var modelManager: ModelManager

    @Inject
    lateinit var isolatedImageDecoder: IsolatedImageDecoder

    @Inject
    lateinit var providerRegistry: ProviderRegistry

    @Inject
    lateinit var cloudProviderInitializer: CloudProviderInitializer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        val onCreateSpan = StartupTracer.begin("App.onCreate")
        // Isolated-process services inherit this Application class but must NOT
        // run Hilt injection (WorkManager/JobScheduler are unavailable there).
        if (getProcessName() != packageName) return

        StartupTracer.trace("App.super.onCreate (Hilt DI)") {
            super.onCreate()
        }

        CloudFetcherRegistryHolder.registry = providerRegistry
        if (BuildConfig.ALLOW_INSECURE_TLS) {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            CloudFetcherRegistryHolder.okHttpClient = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
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

        StartupTracer.end(onCreateSpan)
    }

}