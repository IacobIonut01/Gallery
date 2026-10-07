package com.dot.gallery.cloud.util

import android.net.Uri
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import okhttp3.Request
import java.io.InputStream
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.offline.CloudMediaCache
import com.dot.gallery.domain.uri.path



internal fun resolveCloudDownloadProvider(
    registry: ProviderRegistry,
    providerType: ProviderType,
    configId: Long,
    requireExactAccount: Boolean
): RemoteMediaProvider? {
    val exactProvider = if (configId > 0L) {
        registry.getByConfigId(configId)?.takeIf { it.providerType == providerType }
    } else null
    return (exactProvider ?: if (requireExactAccount) null else registry.get(providerType))
        as? RemoteMediaProvider
}

object CloudMediaDownloader {

    fun downloadCloudMedia(cloudUri: Uri): InputStream? = download(cloudUri, requireExactAccount = false)

    fun downloadCloudMediaExact(cloudUri: Uri): InputStream? =
        download(cloudUri, requireExactAccount = true)

    private fun download(cloudUri: Uri, requireExactAccount: Boolean): InputStream? {
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
        val provider = resolveCloudDownloadProvider(
            registry,
            providerType,
            configId,
            requireExactAccount
        ) ?: return null
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
