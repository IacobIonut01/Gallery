/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.immich.data.api

import okhttp3.Interceptor
import okhttp3.Response

class ImmichAuthInterceptor : Interceptor {

    @Volatile
    var apiKey: String? = null

    @Volatile
    var accessToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        apiKey?.let {
            builder.addHeader("x-api-key", it)
        }

        accessToken?.let {
            if (apiKey == null) {
                builder.addHeader("Authorization", "Bearer $it")
            }
        }

        return chain.proceed(builder.build())
    }
}
