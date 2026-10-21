/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism.ui

import android.net.Uri

/**
 * Helpers for capturing a PhotoPrism access token after browser / WebView login.
 *
 * PhotoPrism's OIDC callback (`auth.gohtml`) writes namespaced keys
 * `pp:<storageNamespace>:session.token` (and `.id`) into localStorage or
 * sessionStorage, then redirects into the app. Password logins use the same keys.
 */
object PhotoPrismSessionTokenExtractor {

    const val CALLBACK_SCHEME = "refragallery"
    const val CALLBACK_HOST = "photoprism"

    /** Deep link: `refragallery://photoprism/callback?token=…` (also `/auth`). */
    fun parseCallbackToken(uri: Uri?): String? {
        if (uri == null) return null
        return parseCallbackToken(
            scheme = uri.scheme,
            host = uri.host,
            path = uri.path,
            token = uri.getQueryParameter("token")
        )
    }

    /**
     * Pure-JVM-friendly overload used by unit tests and deep-link parsing.
     */
    fun parseCallbackToken(
        scheme: String?,
        host: String?,
        path: String?,
        token: String?
    ): String? {
        if (!scheme.equals(CALLBACK_SCHEME, ignoreCase = true)) return null
        if (!host.equals(CALLBACK_HOST, ignoreCase = true)) return null
        val normalized = path.orEmpty().trim('/')
        if (normalized.isNotEmpty() && normalized != "callback" && normalized != "auth") return null
        return token?.trim()?.takeIf { it.isNotBlank() }
    }

    fun isPhotoPrismCallback(uri: Uri?): Boolean {
        if (uri == null) return false
        if (!uri.scheme.equals(CALLBACK_SCHEME, ignoreCase = true)) return false
        if (!uri.host.equals(CALLBACK_HOST, ignoreCase = true)) return false
        return true
    }

    /**
     * Scans WebView localStorage + sessionStorage for a PhotoPrism session token.
     * Returns a JS expression that evaluates to a JSON string (token) or null.
     */
    val EXTRACT_TOKEN_JS: String = """
        (function() {
          function scan(storage) {
            if (!storage) return null;
            try {
              for (var i = 0; i < storage.length; i++) {
                var k = storage.key(i);
                if (!k) continue;
                if (k === 'session.token' || k.indexOf(':session.token') !== -1 || k === 'authToken') {
                  var v = storage.getItem(k);
                  if (v && v !== 'null' && v !== 'undefined' && v.length > 0) return v;
                }
              }
            } catch (e) {}
            return null;
          }
          return scan(window.localStorage) || scan(window.sessionStorage) || null;
        })();
    """.trimIndent()

    /** Decode [android.webkit.WebView.evaluateJavascript] result (JSON-encoded string or `null`). */
    fun decodeEvaluateJavascriptResult(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "null") return null
        var s = raw.trim()
        if (s.length >= 2 && s.first() == '"' && s.last() == '"') {
            s = s.substring(1, s.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\/", "/")
        }
        return s.takeIf { it.isNotBlank() && it != "null" }
    }

    fun loginUrl(serverUrl: String): String {
        val base = serverUrl.trimEnd('/')
        return "$base/library/login"
    }
}
