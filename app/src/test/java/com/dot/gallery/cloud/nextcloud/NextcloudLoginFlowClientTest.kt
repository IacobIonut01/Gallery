package com.dot.gallery.cloud.nextcloud

import com.dot.gallery.cloud.core.auth.InteractiveAuthErrorKind
import com.dot.gallery.cloud.core.auth.InteractiveAuthException
import com.dot.gallery.cloud.core.auth.InteractiveAuthPollResult
import com.dot.gallery.cloud.nextcloud.auth.NextcloudLoginFlowClient
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NextcloudLoginFlowClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: NextcloudLoginFlowClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = NextcloudLoginFlowClient(OkHttpClient(), nowMillis = { 1_000L })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun beginPreservesSubpathAndParsesSession() = runTest {
        val loginUrl = server.url("/nextcloud/login/v2/flow/id").toString()
        val pollUrl = server.url("/nextcloud/login/v2/poll").toString()
        server.enqueue(
            MockResponse().setBody(
                """{"poll":{"token":"secret-token","endpoint":"$pollUrl"},"login":"$loginUrl"}"""
            )
        )

        val session = client.begin(server.url("/nextcloud").toString())

        assertEquals(loginUrl, session.browserUrl)
        assertEquals(pollUrl, session.pollEndpoint)
        assertEquals("/nextcloud/index.php/login/v2", server.takeRequest().path)
    }

    @Test
    fun pollTreats404AsPendingAnd200AsComplete() = runTest {
        val pollUrl = server.url("/login/v2/poll").toString()
        val session = com.dot.gallery.cloud.core.auth.InteractiveAuthSession(
            providerType = com.dot.gallery.cloud.core.ProviderType.NEXTCLOUD,
            browserUrl = server.url("/login").toString(),
            pollEndpoint = pollUrl,
            token = "poll-token",
            trustedOrigin = server.url("/").toString().trimEnd('/'),
            expiresAtMillis = 10_000L
        )
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(
            MockResponse().setBody(
                """{"server":"${server.url("/").toString().trimEnd('/')}","loginName":"alice","appPassword":"app-secret"}"""
            )
        )

        assertEquals(InteractiveAuthPollResult.Pending, client.poll(session))
        val complete = client.poll(session) as InteractiveAuthPollResult.Complete

        assertEquals("alice", complete.credentials.username)
        assertEquals("app-secret", complete.credentials.password)
        assertEquals("token=poll-token", server.takeRequest().body.readUtf8())
        server.takeRequest()
    }

    @Test
    fun expiredSessionStopsBeforePolling() = runTest {
        val session = com.dot.gallery.cloud.core.auth.InteractiveAuthSession(
            providerType = com.dot.gallery.cloud.core.ProviderType.NEXTCLOUD,
            browserUrl = server.url("/login").toString(),
            pollEndpoint = server.url("/login/v2/poll").toString(),
            token = "poll-token",
            trustedOrigin = server.url("/").toString().trimEnd('/'),
            expiresAtMillis = 1_000L
        )

        val error = runCatching { client.poll(session) }.exceptionOrNull()

        assertTrue(error is InteractiveAuthException)
        assertEquals(InteractiveAuthErrorKind.EXPIRED, (error as InteractiveAuthException).kind)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun initiation404ReportsUnsupported() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val error = runCatching { client.begin(server.url("/").toString()) }.exceptionOrNull()

        assertTrue(error is InteractiveAuthException)
        assertEquals(InteractiveAuthErrorKind.UNSUPPORTED, (error as InteractiveAuthException).kind)
    }

    @Test
    fun rejectsCrossOriginPollEndpoint() = runTest {
        val loginUrl = server.url("/login/v2/flow/id").toString()
        server.enqueue(
            MockResponse().setBody(
                """{"poll":{"token":"secret-token","endpoint":"https://evil.example/poll"},"login":"$loginUrl"}"""
            )
        )

        val error = runCatching { client.begin(server.url("/").toString()) }.exceptionOrNull()

        assertTrue(error is InteractiveAuthException)
        assertEquals(InteractiveAuthErrorKind.UNTRUSTED_RESPONSE, (error as InteractiveAuthException).kind)
        assertTrue(error.message.orEmpty().contains("secret-token").not())
    }

    @Test
    fun revokeUsesDeleteAndOcsHeader() = runTest {
        server.enqueue(MockResponse())

        client.revoke(server.url("/nextcloud").toString(), "alice", "app-secret").getOrThrow()

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/nextcloud/ocs/v2.php/core/apppassword", request.path)
        assertEquals("true", request.getHeader("OCS-APIREQUEST"))
        assertTrue(request.getHeader("Authorization").orEmpty().startsWith("Basic "))
    }
}
