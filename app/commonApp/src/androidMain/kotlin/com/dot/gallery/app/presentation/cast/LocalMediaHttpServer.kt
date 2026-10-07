/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.cast

import android.content.Context
import android.net.Uri
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap
import com.dot.gallery.core.presentation.util.clear
import com.dot.gallery.core.presentation.util.remove


/**
 * Lightweight HTTP server that serves local media files to FCast receivers on the LAN.
 * Each media item is registered with a unique token and served at /media/{token}.
 */
class LocalMediaHttpServer(
    private val context: Context,
    port: Int = 0 // 0 = auto-assign
) : NanoHTTPD(port) {

    private data class MediaEntry(
        val uri: Uri?,
        val file: File?,
        val mimeType: String,
        val size: Long
    )

    private val mediaEntries = ConcurrentHashMap<String, MediaEntry>()

    /**
     * Register a content:// URI for serving. Returns the token.
     */
    fun registerUri(token: String, uri: Uri, mimeType: String, size: Long): String {
        mediaEntries[token] = MediaEntry(uri = uri, file = null, mimeType = mimeType, size = size)
        return token
    }

    /**
     * Register a file for serving (e.g., decrypted vault media). Returns the token.
     */
    fun registerFile(token: String, file: File, mimeType: String): String {
        mediaEntries[token] = MediaEntry(uri = null, file = file, mimeType = mimeType, size = file.length())
        return token
    }

    fun unregister(token: String) {
        mediaEntries.remove(token)
    }

    fun unregisterAll() {
        mediaEntries.clear()
    }

    /**
     * Build the full URL that the FCast receiver can use to fetch the media.
     */
    fun getMediaUrl(token: String): String? {
        if (!mediaEntries.containsKey(token)) return null
        val ip = getLocalIpAddress() ?: return null
        return "http://$ip:${listeningPort}/media/$token"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: return newFixedLengthResponse(
            Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found"
        )

        // Expect /media/{token}
        if (!uri.startsWith("/media/")) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found"
            )
        }

        val token = uri.removePrefix("/media/")
        val entry = mediaEntries[token] ?: return newFixedLengthResponse(
            Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found"
        )

        return try {
            val inputStream: InputStream = when {
                entry.file != null -> FileInputStream(entry.file)
                entry.uri != null -> context.contentResolver.openInputStream(entry.uri)
                    ?: return newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot open media"
                    )
                else -> return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "No source"
                )
            }

            val rangeHeader = session.headers["range"]
            if (rangeHeader != null && entry.size > 0) {
                serveRange(inputStream, entry, rangeHeader)
            } else {
                newFixedLengthResponse(
                    Response.Status.OK,
                    entry.mimeType,
                    inputStream,
                    entry.size
                )
            }
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Error: ${e.message}"
            )
        }
    }

    private fun serveRange(
        inputStream: InputStream,
        entry: MediaEntry,
        rangeHeader: String
    ): Response {
        val totalSize = entry.size
        // Parse "bytes=START-END"
        val range = rangeHeader.replace("bytes=", "").trim()
        val parts = range.split("-")
        val start = parts[0].toLongOrNull() ?: 0L
        val end = if (parts.size > 1 && parts[1].isNotBlank()) {
            parts[1].toLong()
        } else {
            totalSize - 1
        }

        val contentLength = end - start + 1
        inputStream.skip(start)

        val response = newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT,
            entry.mimeType,
            inputStream,
            contentLength
        )
        response.addHeader("Content-Range", "bytes $start-$end/$totalSize")
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    companion object {
        fun getLocalIpAddress(): String? {
            return try {
                NetworkInterface.getNetworkInterfaces()?.toList()
                    ?.flatMap { it.inetAddresses.toList() }
                    ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                    ?.hostAddress
            } catch (_: Exception) {
                null
            }
        }
    }
}
