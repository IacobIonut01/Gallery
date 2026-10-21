/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Stub when PhotoPrism is excluded from the build. */
object PhotoPrismSessionTokenExtractor {
    const val CALLBACK_SCHEME = "refragallery"
    const val CALLBACK_HOST = "photoprism"
    const val EXTRACT_TOKEN_JS: String = "null"
    fun parseCallbackToken(uri: Uri?): String? = null
    fun parseCallbackToken(
        scheme: String?,
        host: String?,
        path: String?,
        token: String?
    ): String? = null
    fun isPhotoPrismCallback(uri: Uri?): Boolean = false
    fun decodeEvaluateJavascriptResult(raw: String?): String? = null
    fun loginUrl(serverUrl: String): String = serverUrl
}

/** Stub when PhotoPrism is excluded from the build. */
class PhotoPrismBrowserLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_ACCESS_TOKEN = "access_token"
        fun createIntent(context: Context, serverUrl: String): Intent =
            Intent(context, PhotoPrismBrowserLoginActivity::class.java)
                .putExtra(EXTRA_SERVER_URL, serverUrl)
    }
}
