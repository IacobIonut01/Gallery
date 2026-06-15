/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.owncloud.data.api

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class WebDavResource(
    val href: String,
    val displayName: String = "",
    val contentType: String = "",
    val contentLength: Long = 0L,
    val lastModified: String = "",
    val etag: String = "",
    val isCollection: Boolean = false,
    val ownCloudFileId: String = ""
)

class WebDavClient(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val credentials = Credentials.basic(username, password)

    private val webDavBaseUrl: String
        get() = "${baseUrl.trimEnd('/')}/remote.php/dav/files/$username"

    fun propFind(remotePath: String, depth: Int = 1): List<WebDavResource> {
        val url = "$webDavBaseUrl/${remotePath.trimStart('/')}"
        val propFindBody = """<?xml version="1.0" encoding="UTF-8"?>
            <d:propfind xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns">
                <d:prop>
                    <d:displayname/>
                    <d:getcontenttype/>
                    <d:getcontentlength/>
                    <d:getlastmodified/>
                    <d:getetag/>
                    <d:resourcetype/>
                    <oc:fileid/>
                </d:prop>
            </d:propfind>""".trimIndent()

        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", propFindBody.toRequestBody("application/xml".toMediaType()))
            .header("Authorization", credentials)
            .header("Depth", depth.toString())
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw WebDavException("PROPFIND failed: ${response.code} ${response.message}")
        }
        return parsePropFindResponse(response.body?.string() ?: "")
    }

    fun download(remotePath: String): ByteArray {
        val url = "$webDavBaseUrl/${remotePath.trimStart('/')}"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", credentials)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw WebDavException("Download failed: ${response.code}")
        }
        return response.body?.bytes() ?: ByteArray(0)
    }

    fun upload(remotePath: String, file: File, contentType: String = "application/octet-stream") {
        val url = "$webDavBaseUrl/${remotePath.trimStart('/')}"
        val request = Request.Builder()
            .url(url)
            .put(file.asRequestBody(contentType.toMediaType()))
            .header("Authorization", credentials)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw WebDavException("Upload failed: ${response.code}")
        }
    }

    fun mkdir(remotePath: String) {
        val url = "$webDavBaseUrl/${remotePath.trimStart('/')}"
        val request = Request.Builder()
            .url(url)
            .method("MKCOL", null)
            .header("Authorization", credentials)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful && response.code != 405) { // 405 = already exists
            throw WebDavException("MKCOL failed: ${response.code}")
        }
    }

    fun delete(remotePath: String) {
        val url = "$webDavBaseUrl/${remotePath.trimStart('/')}"
        val request = Request.Builder()
            .url(url)
            .delete()
            .header("Authorization", credentials)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw WebDavException("DELETE failed: ${response.code}")
        }
    }

    fun testConnection(): Boolean {
        return try {
            propFind("", depth = 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getPreviewUrl(fileId: String, width: Int = 256, height: Int = 256): String {
        return "${baseUrl.trimEnd('/')}/index.php/core/preview?fileId=$fileId&x=$width&y=$height&a=true"
    }

    fun getDownloadUrl(remotePath: String): String {
        return "$webDavBaseUrl/${remotePath.trimStart('/')}"
    }

    fun getAuthHeaders(): Map<String, String> = mapOf("Authorization" to credentials)

    private fun parsePropFindResponse(xml: String): List<WebDavResource> {
        if (xml.isBlank()) return emptyList()
        val resources = mutableListOf<WebDavResource>()
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))
            val responses = doc.getElementsByTagNameNS("DAV:", "response")
            for (i in 0 until responses.length) {
                val responseEl = responses.item(i)
                val href = getTextContent(responseEl, "DAV:", "href") ?: continue
                val displayName = getTextContent(responseEl, "DAV:", "displayname") ?: ""
                val contentType = getTextContent(responseEl, "DAV:", "getcontenttype") ?: ""
                val contentLength = getTextContent(responseEl, "DAV:", "getcontentlength")?.toLongOrNull() ?: 0L
                val lastModified = getTextContent(responseEl, "DAV:", "getlastmodified") ?: ""
                val etag = getTextContent(responseEl, "DAV:", "getetag") ?: ""
                val fileId = getTextContent(responseEl, "http://owncloud.org/ns", "fileid") ?: ""

                val resourceTypeNodes = responseEl.let {
                    if (it is org.w3c.dom.Element) {
                        val propStats = it.getElementsByTagNameNS("DAV:", "propstat")
                        var isCol = false
                        for (j in 0 until propStats.length) {
                            val prop = (propStats.item(j) as? org.w3c.dom.Element)
                                ?.getElementsByTagNameNS("DAV:", "prop")?.item(0) as? org.w3c.dom.Element
                            val resourceType = prop?.getElementsByTagNameNS("DAV:", "resourcetype")?.item(0) as? org.w3c.dom.Element
                            if (resourceType != null) {
                                val collection = resourceType.getElementsByTagNameNS("DAV:", "collection")
                                isCol = collection.length > 0
                            }
                        }
                        isCol
                    } else false
                }

                resources.add(
                    WebDavResource(
                        href = href,
                        displayName = displayName.ifBlank { href.trimEnd('/').substringAfterLast('/') },
                        contentType = contentType,
                        contentLength = contentLength,
                        lastModified = lastModified,
                        etag = etag,
                        isCollection = resourceTypeNodes,
                        ownCloudFileId = fileId
                    )
                )
            }
        } catch (_: Exception) {
            // Fallback for malformed XML
        }
        return resources
    }

    private fun getTextContent(parent: org.w3c.dom.Node, namespaceUri: String, localName: String): String? {
        if (parent is org.w3c.dom.Element) {
            val nodes = parent.getElementsByTagNameNS(namespaceUri, localName)
            if (nodes.length > 0) {
                return nodes.item(0).textContent
            }
        }
        return null
    }
}

class WebDavException(message: String) : Exception(message)
