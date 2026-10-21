/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism.data.api

import okhttp3.Interceptor
import okhttp3.Response

enum class PhotoPrismAuthMode {
    /** Long-lived app password or access token stored in apiKey. */
    STATIC_TOKEN,
    /** Username/password session; regenerable via POST /session. */
    SESSION,
    /** OAuth2 client_credentials; regenerable via POST /oauth/token. */
    OAUTH_CLIENT
}

class PhotoPrismAuthInterceptor : Interceptor {

    @Volatile
    var accessToken: String? = null

    @Volatile
    var authMode: PhotoPrismAuthMode = PhotoPrismAuthMode.STATIC_TOKEN

    @Volatile
    var previewToken: String? = null

    @Volatile
    var downloadToken: String? = null

    /** Credentials used by [PhotoPrismAuthenticator] to renew session/OAuth tokens. */
    @Volatile
    var username: String? = null

    @Volatile
    var password: String? = null

    @Volatile
    var baseUrl: String = ""

    fun clear() {
        accessToken = null
        previewToken = null
        downloadToken = null
        username = null
        password = null
        baseUrl = ""
        authMode = PhotoPrismAuthMode.STATIC_TOKEN
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        // Login/token endpoints must not carry a stale Bearer token.
        val isAuthEndpoint = path.endsWith("/api/v1/session") ||
            path.endsWith("/api/v1/sessions") ||
            path.endsWith("/api/v1/oauth/token")

        val builder = original.newBuilder()
        if (!isAuthEndpoint) {
            accessToken?.let { token ->
                builder.header("Authorization", "Bearer $token")
                builder.header("X-Auth-Token", token)
            }
        }
        return chain.proceed(builder.build())
    }
}
