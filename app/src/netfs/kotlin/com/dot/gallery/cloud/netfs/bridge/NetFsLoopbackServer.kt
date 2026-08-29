/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.netfs.bridge

import com.dot.gallery.cloud.core.CloudTrace
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.ThumbnailSize
import com.dot.gallery.cloud.image.CloudFetcherRegistryHolder
import com.dot.gallery.feature_node.presentation.util.printError
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.SecureRandom
import java.util.Base64

/**
 * Stream source the loopback server reads from. Implemented by `NetworkFileSystemProvider`
 * so the server can stay protocol-agnostic and resolve the right provider per request.
 */
interface NetFsLoopbackSource {
    fun loopbackSize(path: String): Long
    fun loopbackOpen(path: String, offset: Long): InputStream
    fun loopbackMime(path: String): String
    fun loopbackThumbnail(path: String, size: ThumbnailSize): ByteArray?
}

internal data class NetFsLoopbackRoute(
    val providerType: ProviderType,
    val configId: Long,
    val kind: String,
    val sizeName: String,
    val path: String
)

internal fun buildNetFsLoopbackPath(
    token: String,
    providerType: ProviderType,
    configId: Long,
    kind: String,
    sizeName: String,
    path: String
): String {
    val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(path.toByteArray())
    return "/$token/${providerType.name}/$configId/$kind/$sizeName/$encoded"
}

internal fun parseNetFsLoopbackRoute(uri: String, expectedToken: String): NetFsLoopbackRoute? {
    val parts = uri.trimStart('/').split('/')
    if (parts.size !in 5..6 || parts[0] != expectedToken) return null
    val providerType = runCatching { ProviderType.valueOf(parts[1]) }.getOrNull() ?: return null
    val hasConfigId = parts.size == 6
    val configId = if (hasConfigId) parts[2].toLongOrNull() ?: return null else -1L
    val offset = if (hasConfigId) 1 else 0
    val path = runCatching {
        String(Base64.getUrlDecoder().decode(parts[4 + offset]))
    }.getOrNull() ?: return null
    return NetFsLoopbackRoute(
        providerType = providerType,
        configId = configId,
        kind = parts[2 + offset],
        sizeName = parts[3 + offset],
        path = path
    )
}

internal data class NetFsByteRange(val start: Long, val end: Long)

internal fun parseNetFsByteRange(header: String, total: Long): NetFsByteRange? {
    if (total <= 0L || !header.startsWith("bytes=") || ',' in header) return null
    val value = header.removePrefix("bytes=").trim()
    val separator = value.indexOf('-')
    if (separator < 0) return null
    val startValue = value.substring(0, separator).trim()
    val endValue = value.substring(separator + 1).trim()
    if (startValue.isEmpty()) {
        val suffixLength = endValue.toLongOrNull()?.takeIf { it > 0L } ?: return null
        return NetFsByteRange((total - suffixLength).coerceAtLeast(0L), total - 1L)
    }
    val start = startValue.toLongOrNull()?.takeIf { it in 0 until total } ?: return null
    val end = if (endValue.isEmpty()) {
        total - 1L
    } else {
        endValue.toLongOrNull()?.takeIf { it >= start }?.coerceAtMost(total - 1L) ?: return null
    }
    return NetFsByteRange(start, end)
}

/**
 * On-device HTTP bridge that exposes SMB/NFS streams as `http://127.0.0.1` URLs so the
 * existing cloud media pipeline (Glide / Sketch / ZoomImage / ExoPlayer) can consume them
 * unchanged, including `Range` seeking for video.
 *
 * URL shape: `http://127.0.0.1:{port}/{token}/{PROVIDER}/{configId}/{kind}/{size}/{base64url(path)}`
 *  - `kind`  = `original` | `thumb`
 *  - `size`  = `orig` | `preview` | `thumbnail`
 * The random per-process [token] prevents other local apps from reading the port.
 */
internal class NetFsLoopbackServer : NanoHTTPD(LOOPBACK_HOST, 0) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: return notFound()
        if (uri.trimStart('/').substringBefore('/') != NetFsLoopback.token) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
        }
        val route = parseNetFsLoopbackRoute(uri, NetFsLoopback.token) ?: return notFound()
        val registry = CloudFetcherRegistryHolder.registry
        val provider = if (route.configId > 0L) {
            registry?.getByConfigId(route.configId)
        } else {
            registry?.get(route.providerType)
        }
        if (provider?.providerType != route.providerType) return notFound()
        val source = provider as? NetFsLoopbackSource
            ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "No provider")

        return try {
            if (route.kind == KIND_THUMB) {
                val size = if (route.sizeName == "thumbnail") ThumbnailSize.THUMBNAIL else ThumbnailSize.PREVIEW
                val bytes = CloudTrace.time("Loopback[${route.providerType}] thumb/${route.sizeName} generate '${route.path}'") {
                    source.loopbackThumbnail(route.path, size)
                } ?: return notFound()
                CloudTrace.d("Loopback[${route.providerType}] thumb/${route.sizeName} '${route.path}' -> ${CloudTrace.bytes(bytes.size.toLong())}")
                newFixedLengthResponse(
                    Response.Status.OK, "image/jpeg",
                    ByteArrayInputStream(bytes), bytes.size.toLong()
                )
            } else {
                serveOriginal(session, source, route.path)
            }
        } catch (e: Exception) {
            // Surface the real cause: Sketch/Glide only log the bare "HTTP 500" status line, so
            // without this the underlying SMB/NFS read failure is invisible.
            printError("NetFsLoopback: ${route.providerType} ${route.kind}/${route.sizeName} failed for '${route.path}': ${e.javaClass.simpleName}: ${e.message}")
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }

    private fun serveOriginal(session: IHTTPSession, source: NetFsLoopbackSource, path: String): Response {
        val total = source.loopbackSize(path)
        val mime = source.loopbackMime(path)
        val rangeHeader = session.headers["range"]

        if (total <= 0L) {
            val stream = TracingInputStream("Loopback original '$path' full", source.loopbackOpen(path, 0L))
            return newChunkedResponse(Response.Status.OK, mime, stream).apply {
                addHeader("Accept-Ranges", "none")
            }
        }

        if (rangeHeader != null) {
            val range = parseNetFsByteRange(rangeHeader, total)
                ?: return newFixedLengthResponse(
                    Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Range Not Satisfiable"
                ).apply { addHeader("Content-Range", "bytes */$total") }
            val length = range.end - range.start + 1L
            CloudTrace.d("Loopback original '$path' range=${range.start}-${range.end}/$total (${CloudTrace.bytes(length)}, $mime)")
            val stream = TracingInputStream(
                "Loopback original '$path' bytes=${range.start}-${range.end}",
                source.loopbackOpen(path, range.start)
            )
            return newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, stream, length).apply {
                addHeader("Content-Range", "bytes ${range.start}-${range.end}/$total")
                addHeader("Accept-Ranges", "bytes")
            }
        }

        CloudTrace.d("Loopback original '$path' full (${CloudTrace.bytes(total)}, $mime)")
        val stream = TracingInputStream("Loopback original '$path' full", source.loopbackOpen(path, 0L))
        return newFixedLengthResponse(Response.Status.OK, mime, stream, total).apply {
            addHeader("Accept-Ranges", "bytes")
        }
    }

    /**
     * Wraps the SMB/NFS stream to log how long the full transfer took and the achieved throughput,
     * so a slow "original" load can be attributed to network read time (vs. decode in the client).
     */
    private class TracingInputStream(
        private val label: String,
        private val delegate: InputStream
    ) : InputStream() {
        private val startNs = System.nanoTime()
        private var transferred = 0L
        private var logged = false

        override fun read(): Int = delegate.read().also { if (it >= 0) transferred++ else logDone() }

        override fun read(b: ByteArray, off: Int, len: Int): Int =
            delegate.read(b, off, len).also { if (it > 0) transferred += it else if (it < 0) logDone() }

        override fun available(): Int = delegate.available()

        override fun close() {
            logDone()
            delegate.close()
        }

        private fun logDone() {
            if (logged) return
            logged = true
            val ms = (System.nanoTime() - startNs) / 1_000_000
            val mbps = if (ms > 0) transferred * 1000.0 / (1024.0 * 1024.0) / ms else 0.0
            CloudTrace.d("$label streamed ${CloudTrace.bytes(transferred)} in ${ms}ms (%.2f MB/s)".format(mbps))
        }
    }

    private fun notFound(): Response =
        newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")

    companion object {
        const val LOOPBACK_HOST = "127.0.0.1"
        const val KIND_ORIGINAL = "original"
        const val KIND_THUMB = "thumb"
    }
}

/**
 * Lazily-started singleton wrapper around [NetFsLoopbackServer]. Providers call
 * [originalUrl] / [thumbnailUrl] to obtain loopback URLs for the media pipeline.
 */
object NetFsLoopback {

    @Volatile
    private var server: NetFsLoopbackServer? = null

    @Volatile
    var token: String = randomToken()
        private set

    @Synchronized
    private fun ensureStarted(): Int {
        var s = server
        if (s == null) {
            // NanoHTTPD logs every client-side disconnect ("Broken pipe" / "Connection reset") at
            // SEVERE with a full stack trace. These are expected whenever the image/video pipeline
            // cancels a request (scrolling, seeking, switching items), so silence them to avoid
            // drowning the log — real serve() failures are returned as HTTP error responses instead.
            java.util.logging.Logger.getLogger(NanoHTTPD::class.java.name).level =
                java.util.logging.Level.OFF
            s = NetFsLoopbackServer()
            s.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
            server = s
        }
        return s.listeningPort
    }

    private fun base(): String = "http://${NetFsLoopbackServer.LOOPBACK_HOST}:${ensureStarted()}"

    fun originalUrl(type: ProviderType, configId: Long, path: String): String =
        base() + buildNetFsLoopbackPath(
            token, type, configId, NetFsLoopbackServer.KIND_ORIGINAL, "orig", path
        )

    fun thumbnailUrl(type: ProviderType, configId: Long, path: String, size: ThumbnailSize): String {
        val sizeName = if (size == ThumbnailSize.THUMBNAIL) "thumbnail" else "preview"
        return base() + buildNetFsLoopbackPath(
            token, type, configId, NetFsLoopbackServer.KIND_THUMB, sizeName, path
        )
    }

    @Synchronized
    fun stop() {
        server?.stop()
        server = null
        token = randomToken()
    }

    private fun randomToken(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
