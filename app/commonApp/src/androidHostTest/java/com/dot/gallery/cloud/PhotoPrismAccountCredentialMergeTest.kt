/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.ui.AddServerUiState
import com.dot.gallery.cloud.ui.mergeCloudServerConfig
import com.dot.gallery.cloud.ui.normalizeCloudServerUrl
import com.dot.gallery.data.entity.CloudServerConfigEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoPrismAccountCredentialMergeTest {

    @Test
    fun browserTokenIsStoredAsApiKeyWithoutPassword() {
        val state = AddServerUiState(
            providerType = ProviderType.PHOTOPRISM,
            serverUrl = "https://pp.example",
            apiKey = "sess",
            username = "",
            password = "should-not-persist"
        )
        val entity = mergeCloudServerConfig(oldEntity = null, state = state) { "enc:$it" }
        assertEquals("enc:sess", entity.apiKey)
        assertNull(entity.encryptedPassword)
        assertNull(entity.username)
    }

    @Test
    fun editKeepsStoredSessionTokenWhenFormHasNoReplacement() {
        val old = CloudServerConfigEntity(
            id = 7L,
            providerType = ProviderType.PHOTOPRISM,
            serverUrl = "https://pp.example",
            apiKey = "enc-sess",
            username = null,
            encryptedPassword = null,
            displayName = "PP"
        )
        val state = AddServerUiState(
            providerType = ProviderType.PHOTOPRISM,
            serverUrl = "https://pp.example",
            apiKey = "",
            username = "",
            password = "",
            displayName = "Renamed",
            savedConfigId = 7L
        )
        val entity = mergeCloudServerConfig(old, state) { "enc:$it" }
        assertEquals("enc-sess", entity.apiKey)
        assertNull(entity.encryptedPassword)
        assertEquals("Renamed", entity.displayName)
    }

    @Test
    fun passwordLoginStillStoresPasswordWhenNoApiKey() {
        val state = AddServerUiState(
            providerType = ProviderType.PHOTOPRISM,
            serverUrl = "https://pp.example",
            username = "user",
            password = "secret"
        )
        val entity = mergeCloudServerConfig(oldEntity = null, state = state) { "enc:$it" }
        assertNull(entity.apiKey)
        assertEquals("enc:secret", entity.encryptedPassword)
        assertEquals("user", entity.username)
    }

    @Test
    fun normalizeCloudServerUrlAddsHttps() {
        assertEquals("https://photos.example.com", normalizeCloudServerUrl("photos.example.com"))
        assertEquals("https://photos.example.com", normalizeCloudServerUrl("  photos.example.com/ "))
        assertEquals("http://photos.example.com", normalizeCloudServerUrl("http://photos.example.com"))
        assertEquals("smb://nas/share", normalizeCloudServerUrl("smb://nas/share"))
    }
}
