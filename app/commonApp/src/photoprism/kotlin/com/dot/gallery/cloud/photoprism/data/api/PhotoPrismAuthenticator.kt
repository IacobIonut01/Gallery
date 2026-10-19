/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism.data.api

import com.dot.gallery.core.logging.printDebug
import okhttp3.Authenticator
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Renews PhotoPrism session or OAuth access tokens on HTTP 401.
 * Static app-password / access-token mode intentionally does not retry.
 */
class PhotoPrismAuthenticator(
    private val authInterceptor: PhotoPrismAuthInterceptor
) : Authenticator {

    private val refreshClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        if (authInterceptor.authMode == PhotoPrismAuthMode.STATIC_TOKEN) return null

        val base = authInterceptor.baseUrl.trimEnd('/')
        val username = authInterceptor.username
        val password = authInterceptor.password
        if (base.isBlank() || username.isNullOrBlank() || password.isNullOrBlank()) return null

        val renewed = when (authInterceptor.authMode) {
            PhotoPrismAuthMode.SESSION -> renewSession(base, username, password)
            PhotoPrismAuthMode.OAUTH_CLIENT -> renewOAuth(base, username, password)
            PhotoPrismAuthMode.STATIC_TOKEN -> null
        } ?: return null

        authInterceptor.accessToken = renewed.accessToken
        renewed.previewToken?.let { authInterceptor.previewToken = it }
        renewed.downloadToken?.let { authInterceptor.downloadToken = it }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${renewed.accessToken}")
            .header("X-Auth-Token", renewed.accessToken)
            .build()
    }

    private fun renewSession(baseUrl: String, username: String, password: String): RenewedTokens? {
        return try {
            val body = JSONObject()
                .put("username", username)
                .put("password", password)
                .toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/v1/session")
                .post(body)
                .header("Content-Type", "application/json")
                .build()
            refreshClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    printDebug("PhotoPrismAuthenticator: session renew failed ${resp.code}")
                    return null
                }
                parseSessionOrConfig(resp.body.string())
            }
        } catch (e: Exception) {
            printDebug("PhotoPrismAuthenticator: session renew error ${e.message}")
            null
        }
    }

    private fun renewOAuth(baseUrl: String, clientId: String, clientSecret: String): RenewedTokens? {
        return try {
            val form = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build()
            val request = Request.Builder()
                .url("$baseUrl/api/v1/oauth/token")
                .post(form)
                .build()
            refreshClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    printDebug("PhotoPrismAuthenticator: oauth renew failed ${resp.code}")
                    return null
                }
                val json = JSONObject(resp.body.string())
                val token = json.optString("access_token").ifBlank { return null }
                RenewedTokens(accessToken = token)
            }
        } catch (e: Exception) {
            printDebug("PhotoPrismAuthenticator: oauth renew error ${e.message}")
            null
        }
    }

    private fun parseSessionOrConfig(raw: String): RenewedTokens? {
        if (raw.isBlank()) return null
        val json = JSONObject(raw)
        // PhotoPrism returns code 32 when a 2FA passcode is required — not renewable here.
        if (json.optInt("code") == 32) return null
        val token = json.optString("access_token")
            .ifBlank { json.optString("id") }
            .ifBlank { return null }
        val config = json.optJSONObject("config")
        return RenewedTokens(
            accessToken = token,
            previewToken = config?.optString("previewToken")?.takeIf { it.isNotBlank() },
            downloadToken = config?.optString("downloadToken")?.takeIf { it.isNotBlank() }
        )
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    data class RenewedTokens(
        val accessToken: String,
        val previewToken: String? = null,
        val downloadToken: String? = null
    )
}
