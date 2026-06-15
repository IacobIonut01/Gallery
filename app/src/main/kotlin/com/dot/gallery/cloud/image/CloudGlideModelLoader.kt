/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.image

import android.net.Uri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import okhttp3.OkHttpClient
import okhttp3.Request
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
        val providerName = model.authority ?: return null
        val providerType = try {
            ProviderType.valueOf(providerName)
        } catch (_: Exception) { return null }

        val remoteId = model.pathSegments.firstOrNull() ?: return null
        val sizeParam = model.getQueryParameter("size") ?: "preview"
        val typeParam = model.getQueryParameter("type")
        val provider = registry.get(providerType) as? RemoteMediaProvider ?: return null

        val url = when (typeParam) {
            "person" -> {
                (provider as? PeopleCapableProvider)?.getPersonThumbnailUrl(remoteId) ?: return null
            }
            else -> when (sizeParam) {
                "thumbnail" -> provider.getThumbnailUrl(remoteId, ThumbnailSize.THUMBNAIL)
                "preview" -> provider.getThumbnailUrl(remoteId, ThumbnailSize.PREVIEW)
                "original" -> provider.getOriginalUrl(remoteId)
                else -> provider.getThumbnailUrl(remoteId, ThumbnailSize.PREVIEW)
            }
        }

        val authHeaders = provider.getAuthHeaders()
        val cacheKey = if (typeParam != null) "${providerType.name}/$typeParam/$remoteId" 
            else "${providerType.name}/$remoteId/$sizeParam"

        return ModelLoader.LoadData(
            ObjectKey(cacheKey),
            CloudOkHttpFetcher(url, authHeaders)
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
    private val authHeaders: Map<String, String>
) : DataFetcher<InputStream> {

    private var call: okhttp3.Call? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val client = CloudFetcherRegistryHolder.okHttpClient ?: OkHttpClient()
        val requestBuilder = Request.Builder().url(url)
        authHeaders.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        call = client.newCall(requestBuilder.build())
        try {
            val response = call!!.execute()
            if (response.isSuccessful) {
                callback.onDataReady(response.body?.byteStream())
            } else {
                callback.onLoadFailed(Exception("HTTP ${response.code}: ${response.message}"))
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
