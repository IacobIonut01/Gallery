/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.photoprism

import com.dot.gallery.cloud.photoprism.ui.PhotoPrismSessionTokenExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoPrismSessionTokenExtractorTest {

    @Test
    fun parseCallbackToken_fromCallbackPath() {
        assertEquals(
            "abc123",
            PhotoPrismSessionTokenExtractor.parseCallbackToken(
                scheme = "refragallery",
                host = "photoprism",
                path = "/callback",
                token = "abc123"
            )
        )
    }

    @Test
    fun parseCallbackToken_fromAuthPath() {
        assertEquals(
            "tok",
            PhotoPrismSessionTokenExtractor.parseCallbackToken(
                scheme = "refragallery",
                host = "photoprism",
                path = "/auth",
                token = "tok"
            )
        )
    }

    @Test
    fun parseCallbackToken_rejectsWrongHost() {
        assertNull(
            PhotoPrismSessionTokenExtractor.parseCallbackToken(
                scheme = "refragallery",
                host = "other",
                path = "/callback",
                token = "x"
            )
        )
    }

    @Test
    fun parseCallbackToken_rejectsBlankToken() {
        assertNull(
            PhotoPrismSessionTokenExtractor.parseCallbackToken(
                scheme = "refragallery",
                host = "photoprism",
                path = "/callback",
                token = "  "
            )
        )
    }

    @Test
    fun parseCallbackToken_allowsEmptyPath() {
        assertEquals(
            "tok",
            PhotoPrismSessionTokenExtractor.parseCallbackToken(
                scheme = "refragallery",
                host = "photoprism",
                path = null,
                token = "tok"
            )
        )
    }

    @Test
    fun decodeEvaluateJavascriptResult_unwrapsJsonString() {
        assertEquals(
            "my-token",
            PhotoPrismSessionTokenExtractor.decodeEvaluateJavascriptResult("\"my-token\"")
        )
        assertNull(PhotoPrismSessionTokenExtractor.decodeEvaluateJavascriptResult("null"))
        assertNull(PhotoPrismSessionTokenExtractor.decodeEvaluateJavascriptResult(null))
    }

    @Test
    fun loginUrl_pointsAtLibraryLogin() {
        assertEquals(
            "https://pp.example/library/login",
            PhotoPrismSessionTokenExtractor.loginUrl("https://pp.example/")
        )
    }
}
