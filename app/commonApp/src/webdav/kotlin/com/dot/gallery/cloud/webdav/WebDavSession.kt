/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.webdav

import android.content.Context
import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.webdav.data.api.OcsApiClient
import com.dot.gallery.cloud.webdav.data.api.WebDavClient
import okhttp3.OkHttpClient


/**
 * Per-connection context handed to a [WebDavDialect]. It exposes the active
 * [WebDavClient] plus the raw connection parameters so dialects can lazily
 * build their own auxiliary clients (e.g. [OcsApiClient] for ownCloud/Nextcloud)
 * without the generic base needing any knowledge of them.
 */
class WebDavSession(
    val context: Context,
    val okHttpClient: OkHttpClient,
    val baseUrl: String,
    val username: String,
    val password: String,
    val config: CloudServerConfig,
    val webDavClient: WebDavClient
) {
    /** Lazily-built OCS client. Only ownCloud/Nextcloud dialects use it. */
    val ocs: OcsApiClient by lazy {
        OcsApiClient(okHttpClient, baseUrl, username, password)
    }
}
