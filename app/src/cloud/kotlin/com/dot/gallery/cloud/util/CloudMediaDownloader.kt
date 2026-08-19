package com.dot.gallery.cloud.util

import android.net.Uri
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import com.dot.gallery.cloud.offline.CloudMediaCache
import okhttp3.Request
import java.io.InputStream

object CloudMediaDownloader {

    fun downloadCloudMedia(cloudUri: Uri): InputStream? {
        val registry = CloudFetcherRegistryHolder.registry ?: return null
        val providerName = cloudUri.authority ?: return null
        // remoteId may contain slashes (SMB/NFS/WebDAV paths like "Photos/IMG.jpg"); pathSegments
        // .first() would truncate it to the first folder and download the directory.
        val remoteId = cloudUri.path?.trimStart('/')?.takeIf { it.isNotEmpty() } ?: return null
        val providerType = try {
            ProviderType.valueOf(providerName)
        } catch (_: Exception) {
            return null
        }
        val configId = cloudUri.getQueryParameter("cfg")?.toLongOrNull() ?: -1L
        val provider = ((if (configId > 0L) registry.getByConfigId(configId) else null)
            ?: registry.get(providerType)) as? RemoteMediaProvider ?: return null
        val url = provider.getOriginalUrl(remoteId)
        val authHeaders = provider.getAuthHeaders()
        val requestBuilder = Request.Builder().url(url).get()
        authHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        requestBuilder.addHeader(
            CloudMediaCache.HEADER_KEY,
            CloudMediaCache.keyFor(providerType, configId, remoteId, "original")
        )
        val client = CloudFetcherRegistryHolder.okHttpClient ?: return null
        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            response.close()
            return null
        }
        return response.body.byteStream()
    }
}
