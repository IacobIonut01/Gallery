/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.image

import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.core.capabilities.PeopleCapableProvider
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.fetch.Fetcher
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.source.ByteArrayDataSource
import com.github.panpf.sketch.source.DataFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Custom Sketch fetcher that resolves cloud:// URIs to authenticated HTTP requests.
 *
 * URI scheme: cloud://{providerType}/{remoteId}[?size=thumbnail|preview|original]
 *
 * Examples:
 *   cloud://IMMICH/abc-123?size=preview
 *   cloud://OWNCLOUD/12345?size=thumbnail
 */
class CloudMediaFetcher private constructor(
    private val providerType: ProviderType,
    private val remoteId: String,
    private val sizeParam: String,
    private val typeParam: String? = null
) : Fetcher {

    override suspend fun fetch(): Result<FetchResult> = withContext(Dispatchers.IO) {
        try {
            val registry = CloudFetcherRegistryHolder.registry
                ?: return@withContext Result.failure(IllegalStateException("ProviderRegistry not available"))

            val provider = registry.get(providerType) as? RemoteMediaProvider
                ?: return@withContext Result.failure(IllegalStateException("No remote provider for $providerType"))

            val url = when (typeParam) {
                "person" -> {
                    (provider as? PeopleCapableProvider)?.getPersonThumbnailUrl(remoteId)
                        ?: return@withContext Result.failure(IllegalStateException("Provider does not support people"))
                }
                else -> when (sizeParam) {
                    "thumbnail" -> provider.getThumbnailUrl(remoteId, ThumbnailSize.THUMBNAIL)
                    "preview" -> provider.getThumbnailUrl(remoteId, ThumbnailSize.PREVIEW)
                    "original" -> provider.getOriginalUrl(remoteId)
                    else -> provider.getThumbnailUrl(remoteId, ThumbnailSize.PREVIEW)
                }
            }

            val authHeaders = provider.getAuthHeaders()

            val requestBuilder = Request.Builder().url(url).get()
            authHeaders.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val client = CloudFetcherRegistryHolder.okHttpClient
                ?: throw IllegalStateException("Cloud OkHttpClient not initialized — ProviderRegistry not ready")

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val bytes = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            val mimeType = response.header("Content-Type")

            Result.success(
                FetchResult(
                    dataSource = ByteArrayDataSource(bytes, DataFrom.NETWORK),
                    mimeType = mimeType
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    class Factory : Fetcher.Factory {

        override val sortWeight: Int = 0

        override fun create(requestContext: RequestContext): Fetcher? {
            val uri = requestContext.request.uri
            val uriString = uri.toString()
            if (!uriString.startsWith("$SCHEME://")) return null

            // Parse cloud://PROVIDER_TYPE/remoteId?size=xxx
            val withoutScheme = uriString.removePrefix("$SCHEME://")
            val authorityEnd = withoutScheme.indexOf('/')
            if (authorityEnd == -1) return null

            val providerName = withoutScheme.substring(0, authorityEnd)
            val providerType = try {
                ProviderType.valueOf(providerName)
            } catch (_: Exception) { return null }

            val pathAndQuery = withoutScheme.substring(authorityEnd + 1)
            val queryStart = pathAndQuery.indexOf('?')
            val remoteId = if (queryStart == -1) pathAndQuery else pathAndQuery.substring(0, queryStart)
            val queryParams = if (queryStart != -1) {
                val queryString = pathAndQuery.substring(queryStart + 1)
                queryString.split('&')
                    .associate { p -> val (k, v) = p.split('=', limit = 2); k to v }
            } else emptyMap()
            val sizeParam = queryParams["size"] ?: "preview"
            val typeParam = queryParams["type"]

            return CloudMediaFetcher(providerType, remoteId, sizeParam, typeParam)
        }

        override fun equals(other: Any?): Boolean = other is Factory
        override fun hashCode(): Int = Factory::class.hashCode()
        override fun toString(): String = "CloudMediaFetcher.Factory"
    }

    companion object {
        const val SCHEME = "cloud"

        fun buildUri(
            providerType: ProviderType,
            remoteId: String,
            size: ThumbnailSize = ThumbnailSize.PREVIEW
        ): String {
            val sizeStr = when (size) {
                ThumbnailSize.THUMBNAIL -> "thumbnail"
                ThumbnailSize.PREVIEW -> "preview"
            }
            return "$SCHEME://${providerType.name}/$remoteId?size=$sizeStr"
        }

        fun buildOriginalUri(providerType: ProviderType, remoteId: String): String {
            return "$SCHEME://${providerType.name}/$remoteId?size=original"
        }

        fun buildPersonUri(providerType: ProviderType, personId: String): String {
            return "$SCHEME://${providerType.name}/$personId?type=person"
        }
    }
}

/**
 * Singleton holder for the registry reference, set during app initialization.
 */
object CloudFetcherRegistryHolder {
    @Volatile
    var registry: ProviderRegistry? = null

    @Volatile
    var okHttpClient: OkHttpClient? = null
}

fun ComponentRegistry.Builder.supportCloudMedia() {
    add(CloudMediaFetcher.Factory())
}
