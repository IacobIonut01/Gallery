/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.media

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import com.dot.gallery.cloud.image.CloudMediaFetcher
import okhttp3.Request

/**
 * ExoPlayer [DataSource] that resolves `cloud://` URIs to authenticated HTTP streams.
 *
 * URI format: `cloud://{ProviderType}/{remoteId}[?size=original|preview|thumbnail]`
 *
 * For video playback the original URL is always used regardless of the `size` parameter,
 * since ExoPlayer needs the full media stream.
 *
 * If the URI is **not** a `cloud://` scheme, the request is delegated to a
 * [DefaultDataSource] so local content:// and file:// URIs still work.
 */
@OptIn(UnstableApi::class)
class CloudDataSource private constructor(
    private val fallbackFactory: DataSource.Factory
) : BaseDataSource(/* isNetwork = */ true) {

    private var activeSource: DataSource? = null
    private var openedCloudStream = false

    // OkHttp call handle for cancellation
    private var activeCall: okhttp3.Call? = null
    private var inputStream: java.io.InputStream? = null
    private var bytesRemaining: Long = 0L

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri

        if (uri.scheme != CloudMediaFetcher.SCHEME) {
            // Delegate non-cloud URIs to the default pipeline
            val fallback = fallbackFactory.createDataSource()
            activeSource = fallback
            return fallback.open(dataSpec)
        }

        // Resolve cloud:// → authenticated HTTP
        val providerName = uri.authority
            ?: throw IllegalArgumentException("Cloud URI missing authority: $uri")
        val providerType = try {
            ProviderType.valueOf(providerName)
        } catch (_: Exception) {
            throw IllegalArgumentException("Unknown cloud provider: $providerName")
        }
        val remoteId = uri.pathSegments.firstOrNull()
            ?: throw IllegalArgumentException("Cloud URI missing remoteId: $uri")

        val registry = CloudFetcherRegistryHolder.registry
            ?: throw IllegalStateException("ProviderRegistry not available")
        val provider = registry.get(providerType) as? RemoteMediaProvider
            ?: throw IllegalStateException("No RemoteMediaProvider for $providerType")

        val url = provider.getOriginalUrl(remoteId)
        val authHeaders = provider.getAuthHeaders()

        val client = CloudFetcherRegistryHolder.okHttpClient ?: okhttp3.OkHttpClient()
        val requestBuilder = Request.Builder().url(url)
        authHeaders.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        // Support range requests for seeking
        if (dataSpec.position > 0) {
            requestBuilder.addHeader("Range", "bytes=${dataSpec.position}-")
        }

        val call = client.newCall(requestBuilder.build())
        activeCall = call
        val response = call.execute()

        if (!response.isSuccessful) {
            response.close()
            throw java.io.IOException("HTTP ${response.code}: ${response.message}")
        }

        val body = response.body
            ?: throw java.io.IOException("Empty response body from $url")

        inputStream = body.byteStream()
        val contentLength = body.contentLength()
        bytesRemaining = if (contentLength > 0) contentLength else Long.MAX_VALUE
        openedCloudStream = true

        transferInitializing(dataSpec)
        transferStarted(dataSpec)

        return if (contentLength > 0) contentLength else androidx.media3.common.C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        activeSource?.let { return it.read(buffer, offset, length) }

        if (bytesRemaining == 0L) return androidx.media3.common.C.RESULT_END_OF_INPUT

        val stream = inputStream
            ?: return androidx.media3.common.C.RESULT_END_OF_INPUT

        val toRead = if (bytesRemaining == Long.MAX_VALUE) length
        else minOf(length.toLong(), bytesRemaining).toInt()

        val bytesRead = stream.read(buffer, offset, toRead)
        if (bytesRead == -1) {
            bytesRemaining = 0
            return androidx.media3.common.C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining != Long.MAX_VALUE) {
            bytesRemaining -= bytesRead
        }
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? {
        return activeSource?.uri
    }

    override fun close() {
        try {
            activeSource?.close()
            activeSource = null
        } finally {
            try {
                inputStream?.close()
                inputStream = null
            } finally {
                activeCall = null
                if (openedCloudStream) {
                    openedCloudStream = false
                    transferEnded()
                }
            }
        }
    }

    /**
     * Factory that creates [CloudDataSource] instances.
     *
     * Usage in ExoPlayer:
     * ```
     * ExoPlayer.Builder(context)
     *     .setMediaSourceFactory(
     *         DefaultMediaSourceFactory(CloudDataSource.Factory(context))
     *     )
     *     .build()
     * ```
     */
    class Factory(
        private val fallbackFactory: DataSource.Factory
    ) : DataSource.Factory {

        constructor(context: android.content.Context) : this(
            DefaultDataSource.Factory(context)
        )

        override fun createDataSource(): DataSource = CloudDataSource(fallbackFactory)
    }
}
