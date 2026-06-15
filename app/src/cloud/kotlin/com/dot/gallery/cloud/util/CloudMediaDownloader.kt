package com.dot.gallery.cloud.util

import android.net.Uri
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import okhttp3.Request
import java.io.InputStream

object CloudMediaDownloader {

    fun downloadCloudMedia(cloudUri: Uri): InputStream? {
        val registry = CloudFetcherRegistryHolder.registry ?: return null
        val providerName = cloudUri.authority ?: return null
        val remoteId = cloudUri.pathSegments.firstOrNull() ?: return null
        val providerType = try {
            ProviderType.valueOf(providerName)
        } catch (_: Exception) {
            return null
        }
        val provider = registry.get(providerType) as? RemoteMediaProvider ?: return null
        val url = provider.getOriginalUrl(remoteId)
        val authHeaders = provider.getAuthHeaders()
        val requestBuilder = Request.Builder().url(url).get()
        authHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        val client = CloudFetcherRegistryHolder.okHttpClient ?: return null
        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) return null
        return response.body?.byteStream()
    }
}
