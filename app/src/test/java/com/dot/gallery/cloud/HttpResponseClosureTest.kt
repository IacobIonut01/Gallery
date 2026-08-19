/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.webdav.data.api.OcsApiClient
import com.dot.gallery.cloud.webdav.data.api.WebDavClient
import com.dot.gallery.cloud.webdav.data.api.WebDavException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class HttpResponseClosureTest {
    private lateinit var server: MockWebServer
    private val closeCount = AtomicInteger()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun webDavWriteOperationsCloseSuccessfulResponses() {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(200).setBody("response")) }
        val file = Files.createTempFile("webdav-upload", ".jpg").toFile()
        val client = webDavClient()

        try {
            client.upload("image.jpg", file)
            client.mkdir("album")
            client.delete("image.jpg")
            client.setFavorite("image.jpg", true)

            assertEquals(4, closeCount.get())
        } finally {
            file.delete()
        }
    }

    @Test
    fun successfulBodyRequestClosesResponseOnce() {
        server.enqueue(
            MockResponse().setResponseCode(207).setBody("<d:multistatus xmlns:d=\"DAV:\"/>")
        )

        webDavClient().propFind("")

        assertEquals(1, closeCount.get())
    }

    @Test
    fun failedRequestsCloseResponses() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("webdav error"))
        server.enqueue(MockResponse().setResponseCode(500).setBody("ocs error"))

        assertThrows(WebDavException::class.java) {
            webDavClient().propFind("")
        }
        assertThrows(Exception::class.java) {
            OcsApiClient(trackingClient(), server.url("/").toString(), "user", "password")
                .getCapabilities()
        }

        assertEquals(2, closeCount.get())
    }

    private fun webDavClient(): WebDavClient = WebDavClient(
        okHttpClient = trackingClient(),
        baseUrl = server.url("/dav").toString(),
        username = "user",
        password = "password",
        filesEndpoint = ""
    )

    private fun trackingClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            val body = response.body
            val trackingSource = object : ForwardingSource(body.source()) {
                private var closed = false

                override fun close() {
                    if (!closed) {
                        closed = true
                        closeCount.incrementAndGet()
                    }
                    super.close()
                }
            }.buffer()
            response.newBuilder()
                .body(object : ResponseBody() {
                    override fun contentType(): MediaType? = body.contentType()
                    override fun contentLength(): Long = body.contentLength()
                    override fun source(): BufferedSource = trackingSource
                })
                .build()
        }
        .build()
}
