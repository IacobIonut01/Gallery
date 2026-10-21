/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.core.util.getString
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_photoprism_browser_confirm
import com.dot.gallery.generated.resources.cloud_photoprism_browser_hint
import com.dot.gallery.generated.resources.cloud_photoprism_browser_title
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-app WebView that completes PhotoPrism login (including Authentik OIDC when PP is the RP).
 *
 * After PhotoPrism's OIDC callback (`auth.gohtml`) writes namespaced `session.token` into
 * Web storage, this activity reads the token, validates it against `/api/v1/config`, and
 * returns it as [EXTRA_ACCESS_TOKEN]. Also accepts the deep link
 * `refragallery://photoprism/callback?token=…` (validated when [EXTRA_SERVER_URL] is known).
 *
 * No username/password is stored — only the captured access token (persisted as encrypted apiKey).
 */
class PhotoPrismBrowserLoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var serverUrl: String
    private val done = AtomicBoolean(false)
    private val bridging = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (done.get() || isFinishing) return
            tryExtractFromStorage()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        serverUrl = intent.getStringExtra(EXTRA_SERVER_URL)?.trim().orEmpty()

        // Deep-link entry: refragallery://photoprism/callback?token=
        val fromIntent = PhotoPrismSessionTokenExtractor.parseCallbackToken(intent?.data)
        if (!fromIntent.isNullOrBlank()) {
            if (serverUrl.isBlank()) {
                // Cold-start via deep link alone cannot validate or return to the add-server flow.
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }
            validateAndFinish(fromIntent)
            // Keep going only if validation is async; UI still needed if validation fails.
        }

        if (serverUrl.isBlank()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val toolbar = Toolbar(this).apply {
            title = getString(Res.string.cloud_photoprism_browser_title)
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { cancelAndFinish() }
            setBackgroundColor(Color.TRANSPARENT)
        }
        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val hint = TextView(this).apply {
            text = getString(Res.string.cloud_photoprism_browser_hint)
            setPadding(48, 24, 48, 24)
            textSize = 13f
        }
        root.addView(hint)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
        }
        root.addView(
            progress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val webContainer = FrameLayout(this)
        webView = createWebView()
        webContainer.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            webContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val confirm = TextView(this).apply {
            text = getString(Res.string.cloud_photoprism_browser_confirm)
            setPadding(48, 36, 48, 48)
            textSize = 14f
            setOnClickListener { tryExtractFromStorage(forceBridge = true) }
        }
        root.addView(confirm)

        setContentView(root)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) webView.goBack()
                    else cancelAndFinish()
                }
            }
        )

        webView.loadUrl(PhotoPrismSessionTokenExtractor.loginUrl(serverUrl))
        handler.postDelayed(pollRunnable, POLL_MS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PhotoPrismSessionTokenExtractor.parseCallbackToken(intent.data)?.let { validateAndFinish(it) }
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        if (::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        io.shutdownNow()
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val view = WebView(this)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = "$userAgentString ReFraGallery/PhotoPrismLogin"
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val token = PhotoPrismSessionTokenExtractor.parseCallbackToken(uri)
                if (token != null) {
                    validateAndFinish(token)
                    return true
                }
                if (PhotoPrismSessionTokenExtractor.isPhotoPrismCallback(uri)) {
                    // Callback without token — ignore and stay in WebView.
                    return true
                }
                return false
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val uri = url?.let(Uri::parse) ?: return false
                val token = PhotoPrismSessionTokenExtractor.parseCallbackToken(uri)
                if (token != null) {
                    validateAndFinish(token)
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.isVisible = false
                tryExtractFromStorage()
                if (url.isNullOrBlank()) return
                // After OIDC HTML writes storage it redirects quickly — poll harder briefly.
                if (url.contains("/api/v1/oauth/oidc", ignoreCase = true) ||
                    url.contains("/api/v1/oidc", ignoreCase = true) ||
                    url.contains("oidc", ignoreCase = true)
                ) {
                    handler.postDelayed({ tryExtractFromStorage() }, 400)
                    handler.postDelayed({ tryExtractFromStorage() }, 1200)
                }
                // Once back on the PhotoPrism app (post-login), also try the cookie session bridge.
                if (isSameOrigin(url) && looksPostLogin(url)) {
                    handler.postDelayed({ tryExtractFromStorage(forceBridge = true) }, 800)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progress.isVisible = true
            }
        }
        return view
    }

    private fun isSameOrigin(url: String): Boolean {
        val base = runCatching { Uri.parse(serverUrl) }.getOrNull() ?: return false
        val page = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        return base.host.equals(page.host, ignoreCase = true)
    }

    private fun looksPostLogin(url: String): Boolean {
        val path = runCatching { Uri.parse(url).path }.getOrNull().orEmpty().lowercase()
        if (path.contains("/library/login") || path.contains("/auth") || path.contains("oidc")) {
            return false
        }
        return path.contains("/library") || path == "/" || path.isEmpty()
    }

    private fun tryExtractFromStorage(forceBridge: Boolean = false) {
        if (done.get() || !::webView.isInitialized) return
        webView.evaluateJavascript(PhotoPrismSessionTokenExtractor.EXTRACT_TOKEN_JS) { raw ->
            val token = PhotoPrismSessionTokenExtractor.decodeEvaluateJavascriptResult(raw)
            if (!token.isNullOrBlank()) {
                validateAndFinish(token)
            } else if (forceBridge) {
                bridgeSessionViaCookies()
            }
        }
    }

    private fun bridgeSessionViaCookies() {
        if (done.get() || !bridging.compareAndSet(false, true)) return
        val base = serverUrl.trimEnd('/')
        CookieManager.getInstance().flush()
        val cookie = CookieManager.getInstance().getCookie(base).orEmpty()
        io.execute {
            try {
                val req = Request.Builder()
                    .url("$base/api/v1/session")
                    .get()
                    .apply {
                        if (cookie.isNotBlank()) header("Cookie", cookie)
                    }
                    .build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        printDebug("PhotoPrismBrowserLogin: session bridge ${resp.code}")
                        return@execute
                    }
                    val body = resp.body.string()
                    val json = JSONObject(body)
                    val token = json.optString("access_token")
                        .ifBlank { json.optString("id") }
                        .takeIf { it.isNotBlank() }
                    if (token != null) {
                        // Cookie bridge already returned a live session — accept directly.
                        runOnUiThread { finishWithToken(token) }
                    }
                }
            } catch (e: Exception) {
                printDebug("PhotoPrismBrowserLogin: session bridge error ${e.message}")
            } finally {
                bridging.set(false)
            }
        }
    }

    private fun validateAndFinish(token: String) {
        if (done.get()) return
        val base = serverUrl.trimEnd('/')
        io.execute {
            val ok = try {
                val req = Request.Builder()
                    .url("$base/api/v1/config")
                    .get()
                    .header("Authorization", "Bearer $token")
                    .header("X-Auth-Token", token)
                    .build()
                http.newCall(req).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                printDebug("PhotoPrismBrowserLogin: validate error ${e.message}")
                // Still accept the token — config may be briefly unavailable mid-redirect.
                true
            }
            if (ok) {
                runOnUiThread { finishWithToken(token) }
            }
        }
    }

    private fun finishWithToken(token: String) {
        if (!done.compareAndSet(false, true)) return
        handler.removeCallbacks(pollRunnable)
        // Drop WebView auth state for this server so cookies/storage are not left behind.
        // The app persists the bearer token encrypted as apiKey.
        runCatching { clearWebAuthState() }
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(EXTRA_ACCESS_TOKEN, token)
        )
        finish()
    }

    private fun clearWebAuthState() {
        val base = serverUrl.trimEnd('/')
        val hostUri = runCatching { Uri.parse(base) }.getOrNull()
        val cm = CookieManager.getInstance()
        if (hostUri?.host != null) {
            val cookie = cm.getCookie(base).orEmpty()
            if (cookie.isNotBlank()) {
                cookie.split(';').forEach { part ->
                    val name = part.substringBefore('=').trim()
                    if (name.isNotEmpty()) {
                        cm.setCookie(base, "$name=; Max-Age=0; Path=/")
                        val secure = base.startsWith("https", ignoreCase = true)
                        if (secure) {
                            cm.setCookie(base, "$name=; Max-Age=0; Path=/; Secure")
                        }
                    }
                }
            }
            cm.flush()
        }
        if (::webView.isInitialized) {
            webView.evaluateJavascript(
                "(function(){try{localStorage.clear();sessionStorage.clear();}catch(e){}})();",
                null
            )
        }
    }

    private fun cancelAndFinish() {
        if (!done.compareAndSet(false, true)) return
        handler.removeCallbacks(pollRunnable)
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_ACCESS_TOKEN = "access_token"
        private const val POLL_MS = 1500L

        fun createIntent(context: Context, serverUrl: String): Intent =
            Intent(context, PhotoPrismBrowserLoginActivity::class.java)
                .putExtra(EXTRA_SERVER_URL, serverUrl)
    }
}
