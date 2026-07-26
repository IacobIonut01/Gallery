package com.dot.gallery.cloud.nextcloud.auth

import com.dot.gallery.BuildConfig
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.auth.InteractiveAuthCredentials
import com.dot.gallery.cloud.core.auth.InteractiveAuthErrorKind
import com.dot.gallery.cloud.core.auth.InteractiveAuthException
import com.dot.gallery.cloud.core.auth.InteractiveAuthPollResult
import com.dot.gallery.cloud.core.auth.InteractiveAuthSession
import com.dot.gallery.cloud.webdav.data.api.buildWebDavOkHttp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class NextcloudLoginFlowClient(
    private val client: okhttp3.OkHttpClient = buildWebDavOkHttp(30),
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun begin(serverUrl: String): InteractiveAuthSession {
        val baseUrl = parseHttpUrl(serverUrl)
        val requestUrl = baseUrl.newBuilder()
            .addPathSegment("index.php")
            .addPathSegment("login")
            .addPathSegment("v2")
            .build()
        val request = Request.Builder()
            .url(requestUrl)
            .post(ByteArray(0).toRequestBody())
            .header("User-Agent", userAgent)
            .build()
        return executeSafely {
            client.newCall(request).execute().use { response ->
                if (response.code == 404 || response.code == 405) {
                    throw InteractiveAuthException(
                        InteractiveAuthErrorKind.UNSUPPORTED,
                        "Nextcloud Login Flow v2 is unavailable"
                    )
                }
                throwForStatus(response.code)
                val payload = decode<BeginResponse>(response.body.string())
                val trustedOrigin = response.request.url.origin()
                val loginUrl = requireTrustedUrl(payload.login, trustedOrigin)
                val pollUrl = requireTrustedUrl(payload.poll.endpoint, trustedOrigin)
                if (payload.poll.token.isBlank()) malformed()
                InteractiveAuthSession(
                    providerType = ProviderType.NEXTCLOUD,
                    browserUrl = loginUrl.toString(),
                    pollEndpoint = pollUrl.toString(),
                    token = payload.poll.token,
                    trustedOrigin = trustedOrigin,
                    expiresAtMillis = nowMillis() + LOGIN_TIMEOUT_MILLIS
                )
            }
        }
    }

    suspend fun poll(session: InteractiveAuthSession): InteractiveAuthPollResult {
        if (nowMillis() >= session.expiresAtMillis) {
            throw InteractiveAuthException(InteractiveAuthErrorKind.EXPIRED, "Nextcloud login expired")
        }
        val pollUrl = requireTrustedUrl(session.pollEndpoint, session.trustedOrigin)
        val request = Request.Builder()
            .url(pollUrl)
            .post(FormBody.Builder().add("token", session.token).build())
            .header("User-Agent", userAgent)
            .build()
        return executeSafely {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@use InteractiveAuthPollResult.Pending
                throwForStatus(response.code)
                val payload = decode<PollResponse>(response.body.string())
                val canonicalServer = requireTrustedUrl(payload.server, session.trustedOrigin)
                if (payload.loginName.isBlank() || payload.appPassword.isBlank()) malformed()
                InteractiveAuthPollResult.Complete(
                    InteractiveAuthCredentials(
                        serverUrl = canonicalServer.toString().trimEnd('/'),
                        username = payload.loginName,
                        password = payload.appPassword
                    )
                )
            }
        }
    }

    suspend fun revoke(serverUrl: String, username: String, appPassword: String): Result<Unit> = runCatching {
        val baseUrl = parseHttpUrl(serverUrl)
        val requestUrl = baseUrl.newBuilder()
            .addPathSegment("ocs")
            .addPathSegment("v2.php")
            .addPathSegment("core")
            .addPathSegment("apppassword")
            .build()
        val request = Request.Builder()
            .url(requestUrl)
            .delete()
            .header("Authorization", Credentials.basic(username, appPassword))
            .header("OCS-APIREQUEST", "true")
            .header("User-Agent", userAgent)
            .build()
        executeSafely {
            client.newCall(request).execute().use { response ->
                throwForStatus(response.code)
            }
        }
    }

    private fun parseHttpUrl(value: String): HttpUrl {
        val url = value.trim().trimEnd('/').toHttpUrlOrNull()
            ?: throw InteractiveAuthException(InteractiveAuthErrorKind.INVALID_URL, "Invalid Nextcloud URL")
        if (url.scheme != "http" && url.scheme != "https") {
            throw InteractiveAuthException(InteractiveAuthErrorKind.INVALID_URL, "Invalid Nextcloud URL")
        }
        return url
    }

    private fun requireTrustedUrl(value: String, trustedOrigin: String): HttpUrl {
        val url = parseHttpUrl(value)
        if (url.origin() != trustedOrigin) {
            throw InteractiveAuthException(
                InteractiveAuthErrorKind.UNTRUSTED_RESPONSE,
                "Nextcloud returned an untrusted login address"
            )
        }
        return url
    }

    private inline fun <T> executeSafely(block: () -> T): T = try {
        block()
    } catch (error: InteractiveAuthException) {
        throw error
    } catch (error: SSLException) {
        throw InteractiveAuthException(InteractiveAuthErrorKind.TLS, "Secure connection failed", error)
    } catch (error: UnknownHostException) {
        throw InteractiveAuthException(InteractiveAuthErrorKind.NETWORK, "Nextcloud server was not found", error)
    } catch (error: SocketTimeoutException) {
        throw InteractiveAuthException(InteractiveAuthErrorKind.NETWORK, "Nextcloud connection timed out", error)
    } catch (error: IOException) {
        throw InteractiveAuthException(InteractiveAuthErrorKind.NETWORK, "Nextcloud connection failed", error)
    }

    private inline fun <reified T> decode(value: String): T = try {
        json.decodeFromString(value)
    } catch (error: Exception) {
        throw InteractiveAuthException(
            InteractiveAuthErrorKind.MALFORMED_RESPONSE,
            "Nextcloud returned an invalid login response",
            error
        )
    }

    private fun throwForStatus(status: Int) {
        if (status in 200..299) return
        val kind = when (status) {
            401, 403 -> InteractiveAuthErrorKind.AUTHENTICATION
            429 -> InteractiveAuthErrorKind.RATE_LIMITED
            in 500..599 -> InteractiveAuthErrorKind.SERVER
            else -> InteractiveAuthErrorKind.SERVER
        }
        throw InteractiveAuthException(kind, "Nextcloud login request failed ($status)")
    }

    private fun malformed(): Nothing = throw InteractiveAuthException(
        InteractiveAuthErrorKind.MALFORMED_RESPONSE,
        "Nextcloud returned an incomplete login response"
    )

    private fun HttpUrl.origin(): String = buildString {
        append(scheme)
        append("://")
        append(host)
        if (port != HttpUrl.defaultPort(scheme)) append(":$port")
    }

    @Serializable
    private data class BeginResponse(
        val poll: PollInfo,
        val login: String
    )

    @Serializable
    private data class PollInfo(
        val token: String,
        val endpoint: String
    )

    @Serializable
    private data class PollResponse(
        val server: String,
        val loginName: String,
        @SerialName("appPassword") val appPassword: String
    )

    private companion object {
        const val LOGIN_TIMEOUT_MILLIS = 20 * 60 * 1000L
        val userAgent = "ReFra/${BuildConfig.VERSION_NAME}"
    }
}
