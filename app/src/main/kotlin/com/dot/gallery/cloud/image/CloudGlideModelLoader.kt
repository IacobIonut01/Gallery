/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.image

import android.net.Uri
import android.util.Log
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.dot.gallery.cloud.core.CloudTrace
import com.dot.gallery.cloud.core.CloudUri
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.resolveRemote
import com.dot.gallery.cloud.offline.CloudMediaCache
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Glide ModelLoader that resolves cloud:// URIs to authenticated HTTP requests
 * using the shared [CloudFetcherRegistryHolder.okHttpClient] (which supports
 * insecure TLS on debug/staging builds).
 */
class CloudGlideModelLoader : ModelLoader<Uri, InputStream> {

    override fun buildLoadData(
        model: Uri,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        if (model.scheme != CloudMediaFetcher.SCHEME) return null

        val registry = CloudFetcherRegistryHolder.registry ?: return null

        // Shared slash-tolerant parse (remoteId may contain '/', e.g. WebDAV "Photos/IMG.jpg").
        val parsed = CloudUri.parse(model.toString()) ?: return null
        val providerType = parsed.providerType
        val remoteId = parsed.remoteId
        val typeParam = parsed.typeParam
        val fileId = parsed.fileId
        val configId = parsed.configId
        val provider = registry.resolveRemote(providerType, configId) ?: return null

        // Glide only renders grid/album thumbnails for cloud media (the media viewer uses Sketch).
        // When Glide requests a small target (a grid cell), fetch the small THUMBNAIL instead of
        // the larger PREVIEW — much less to download and, on WebDAV-family `core/preview`
        // endpoints, much cheaper for the server to generate. `original` and person thumbnails are
        // left untouched. A non-positive size (Target.SIZE_ORIGINAL) keeps the requested size.
        val effectiveSize = parsed.effectiveSize(maxOf(width, height))

        val url = when (typeParam) {
            "person" -> {
                (provider as? PeopleCapableProvider)?.getPersonThumbnailUrl(remoteId) ?: return null
            }
            else -> when (effectiveSize) {
                "thumbnail" -> provider.getThumbnailUrl(remoteId, ThumbnailSize.THUMBNAIL, fileId)
                "preview" -> provider.getThumbnailUrl(remoteId, ThumbnailSize.PREVIEW, fileId)
                "original" -> provider.getOriginalUrl(remoteId)
                else -> provider.getThumbnailUrl(remoteId, ThumbnailSize.PREVIEW, fileId)
            }
        }

        val acct = if (configId > 0L) "$configId/" else ""

        // No server preview URL. For videos on path-based stores (SMB/NFS/WebDAV) we can
        // still decode a poster frame locally from the original stream. Serve that instead
        // of returning null (which drops through to loaders that can only fail to decode).
        if (url.isBlank()) {
            if (typeParam == null && effectiveSize != "original") {
                val size = if (effectiveSize == "thumbnail") ThumbnailSize.THUMBNAIL else ThumbnailSize.PREVIEW
                return ModelLoader.LoadData(
                    ObjectKey("${providerType.name}/$acct$remoteId/videoframe/$effectiveSize"),
                    CloudVideoFrameFetcher(provider, remoteId, size)
                )
            }
            return null
        }

        val authHeaders = provider.getAuthHeaders()
        val cacheKey = if (typeParam != null) "${providerType.name}/$acct$typeParam/$remoteId"
            else "${providerType.name}/$acct$remoteId/$effectiveSize"

        val offlineKey = CloudMediaCache.keyFor(providerType, configId, remoteId, effectiveSize, typeParam)
        return ModelLoader.LoadData(
            ObjectKey(cacheKey),
            CloudOkHttpFetcher(url, authHeaders, offlineKey)
        )
    }

    override fun handles(model: Uri): Boolean = model.scheme == CloudMediaFetcher.SCHEME

    class Factory : ModelLoaderFactory<Uri, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Uri, InputStream> =
            CloudGlideModelLoader()

        override fun teardown() {}
    }
}

/**
 * Glide DataFetcher that uses the shared OkHttpClient from [CloudFetcherRegistryHolder],
 * inheriting its TLS configuration (insecure on debug/staging).
 */
private class CloudOkHttpFetcher(
    private val url: String,
    private val authHeaders: Map<String, String>,
    private val offlineKey: String
) : DataFetcher<InputStream> {

    private var call: okhttp3.Call? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val client = CloudFetcherRegistryHolder.okHttpClient ?: OkHttpClient()
        val requestBuilder = Request.Builder().url(url)
        authHeaders.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
        requestBuilder.addHeader(CloudMediaCache.HEADER_KEY, offlineKey)

        call = client.newCall(requestBuilder.build())
        try {
            CloudTrace.d("Glide.fetch -> GET $url")
            val start = System.nanoTime()
            call!!.execute().use { response ->
                if (!response.isSuccessful) {
                    CloudTrace.w("Glide.fetch -> HTTP ${response.code} for $url")
                    callback.onLoadFailed(Exception("HTTP ${response.code}: ${response.message}"))
                    return
                }
                val contentType = response.body.contentType()?.toString()
                    ?: response.header("Content-Type")
                val bytes = response.body.bytes()
                if (bytes.isEmpty()) {
                    callback.onLoadFailed(Exception("Empty response body (Content-Type=$contentType)"))
                    return
                }
                CloudTrace.d("Glide.fetch <- ${CloudTrace.bytes(bytes.size.toLong())} ($contentType) in ${(System.nanoTime() - start) / 1_000_000}ms for $url")
                // Servers sometimes answer a preview request with a non-image payload
                // (HTML login/redirect page, JSON error, or an SVG mimetype icon when
                // no real preview exists). Feeding those bytes to Glide produces a long
                // chain of useless decode failures, so reject them up front with a
                // descriptive error and log what was actually returned.
                val isImage = contentType?.startsWith("image/", ignoreCase = true) == true
                if (!isImage) {
                    val snippet = String(bytes.copyOf(minOf(bytes.size, 180)))
                        .replace('\n', ' ').replace('\r', ' ').trim()
                    Log.w(
                        "CloudFetcher",
                        "Non-image preview response: url=$url contentType=$contentType " +
                            "bytes=${bytes.size} snippet=\"$snippet\""
                    )
                    callback.onLoadFailed(
                        Exception("Server returned non-image content (Content-Type=$contentType)")
                    )
                    return
                }
                callback.onDataReady(ByteArrayInputStream(bytes))
            }
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {}
    override fun cancel() { call?.cancel() }
    override fun getDataClass(): Class<InputStream> = InputStream::class.java
    override fun getDataSource(): DataSource = DataSource.REMOTE
}

/**
 * Glide DataFetcher that produces a locally-decoded video poster frame for items that have
 * no server-side preview (videos on path-based stores like SMB/NFS/WebDAV). Delegates to
 * [RemoteMediaProvider.getVideoThumbnailBytes]. Runs on Glide's background thread, so the
 * blocking bridge to the provider's suspend API is safe here.
 */
private class CloudVideoFrameFetcher(
    private val provider: RemoteMediaProvider,
    private val remoteId: String,
    private val size: ThumbnailSize
) : DataFetcher<InputStream> {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            val bytes = runBlocking { provider.getVideoThumbnailBytes(remoteId, size) }
            if (bytes != null && bytes.isNotEmpty()) {
                callback.onDataReady(ByteArrayInputStream(bytes))
            } else {
                callback.onLoadFailed(NoPreviewAvailableException(remoteId))
            }
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {}
    override fun cancel() {}
    override fun getDataClass(): Class<InputStream> = InputStream::class.java
    override fun getDataSource(): DataSource = DataSource.LOCAL
}
