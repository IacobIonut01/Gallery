/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.image

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.github.panpf.zoomimage.subsampling.ImageSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Source

/**
 * [ImageSource] for cloud media that downloads the **original** image
 * via the shared [CloudFetcherRegistryHolder.okHttpClient] (insecure TLS on debug/staging).
 *
 * Used by ZoomImage's subsampling pipeline so that high-resolution cloud images
 * can be tiled/decoded region-by-region without loading the whole bitmap into memory.
 */
class CloudImageSource(
    private val providerType: ProviderType,
    private val remoteId: String,
) : ImageSource {

    override val key: String = "cloud://${providerType.name}/$remoteId?size=original"

    override fun openSource(): Source {
        val registry = CloudFetcherRegistryHolder.registry
            ?: throw IllegalStateException("ProviderRegistry not available")

        val provider = registry.get(providerType) as? RemoteMediaProvider
            ?: throw IllegalStateException("No remote provider for $providerType")

        val url = provider.getOriginalUrl(remoteId)
        val authHeaders = provider.getAuthHeaders()

        val requestBuilder = Request.Builder().url(url).get()
        authHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        val client = CloudFetcherRegistryHolder.okHttpClient ?: OkHttpClient()
        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }

        val body = response.body
            ?: throw Exception("Empty response body")

        return body.source()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CloudImageSource
        return providerType == other.providerType && remoteId == other.remoteId
    }

    override fun hashCode(): Int {
        var result = providerType.hashCode()
        result = 31 * result + remoteId.hashCode()
        return result
    }

    override fun toString(): String = "CloudImageSource(cloud://${providerType.name}/$remoteId)"
}
