/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.owncloud.data.api

import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class OcsShareInfo(
    val id: String,
    val url: String,
    val token: String = "",
    val path: String = "",
    val permissions: Int = 1
)

data class OcsCapabilities(
    val version: String = "",
    val versionString: String = "",
    val edition: String = ""
)

data class OcsUserInfo(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val quota: OcsQuota = OcsQuota()
)

data class OcsQuota(
    val used: Long = 0L,
    val total: Long = 0L,
    val free: Long = 0L
)

class OcsApiClient(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val credentials = Credentials.basic(username, password)
    private val apiBase get() = "${baseUrl.trimEnd('/')}/ocs/v2.php"

    fun getCapabilities(): OcsCapabilities {
        val request = buildOcsRequest("$apiBase/cloud/capabilities")
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("OCS capabilities failed: ${response.code}")
        return parseCapabilities(response.body?.string() ?: "")
    }

    fun getCurrentUser(): OcsUserInfo {
        val request = buildOcsRequest("$apiBase/cloud/user")
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("OCS user info failed: ${response.code}")
        return parseUserInfo(response.body?.string() ?: "")
    }

    fun createPublicShare(
        path: String,
        permissions: Int = 1, // 1=read
        expirationDate: String? = null
    ): OcsShareInfo {
        val bodyBuilder = FormBody.Builder()
            .add("shareType", "3") // public link
            .add("path", path)
            .add("permissions", permissions.toString())

        expirationDate?.let { bodyBuilder.add("expireDate", it) }

        val request = Request.Builder()
            .url("$apiBase/apps/files_sharing/api/v1/shares")
            .post(bodyBuilder.build())
            .header("Authorization", credentials)
            .header("OCS-APIREQUEST", "true")
            .header("Accept", "application/xml")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Create share failed: ${response.code}")
        return parseShareResponse(response.body?.string() ?: "")
    }

    private fun buildOcsRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .get()
            .header("Authorization", credentials)
            .header("OCS-APIREQUEST", "true")
            .header("Accept", "application/xml")
            .build()
    }

    private fun parseCapabilities(xml: String): OcsCapabilities {
        val doc = parseXml(xml) ?: return OcsCapabilities()
        val version = doc.getElementsByTagName("version")
        if (version.length > 0) {
            val versionEl = version.item(0) as? org.w3c.dom.Element
            return OcsCapabilities(
                version = getEl(versionEl, "string") ?: "",
                versionString = getEl(versionEl, "string") ?: "",
                edition = getEl(versionEl, "edition") ?: ""
            )
        }
        return OcsCapabilities()
    }

    private fun parseUserInfo(xml: String): OcsUserInfo {
        val doc = parseXml(xml) ?: return OcsUserInfo()
        val data = doc.getElementsByTagName("data")
        if (data.length > 0) {
            val dataEl = data.item(0) as? org.w3c.dom.Element
            val quotaEl = dataEl?.getElementsByTagName("quota")?.item(0) as? org.w3c.dom.Element
            return OcsUserInfo(
                id = getEl(dataEl, "id") ?: "",
                email = getEl(dataEl, "email") ?: "",
                displayName = getEl(dataEl, "display-name") ?: getEl(dataEl, "displayname") ?: "",
                quota = OcsQuota(
                    used = getEl(quotaEl, "used")?.toLongOrNull() ?: 0L,
                    total = getEl(quotaEl, "total")?.toLongOrNull() ?: 0L,
                    free = getEl(quotaEl, "free")?.toLongOrNull() ?: 0L
                )
            )
        }
        return OcsUserInfo()
    }

    private fun parseShareResponse(xml: String): OcsShareInfo {
        val doc = parseXml(xml) ?: return OcsShareInfo(id = "", url = "")
        val data = doc.getElementsByTagName("data")
        if (data.length > 0) {
            val dataEl = data.item(0) as? org.w3c.dom.Element
            return OcsShareInfo(
                id = getEl(dataEl, "id") ?: "",
                url = getEl(dataEl, "url") ?: "",
                token = getEl(dataEl, "token") ?: "",
                path = getEl(dataEl, "path") ?: "",
                permissions = getEl(dataEl, "permissions")?.toIntOrNull() ?: 1
            )
        }
        return OcsShareInfo(id = "", url = "")
    }

    private fun parseXml(xml: String): org.w3c.dom.Document? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            builder.parse(InputSource(StringReader(xml)))
        } catch (_: Exception) {
            null
        }
    }

    private fun getEl(parent: org.w3c.dom.Element?, tagName: String): String? {
        parent ?: return null
        val nodes = parent.getElementsByTagName(tagName)
        return if (nodes.length > 0) nodes.item(0).textContent else null
    }
}
