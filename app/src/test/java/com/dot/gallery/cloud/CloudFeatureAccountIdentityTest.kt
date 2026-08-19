/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SharedLinkInfo
import com.dot.gallery.feature_node.presentation.util.Screen
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudFeatureAccountIdentityTest {

    @Test
    fun sameProviderRemoteIdsHaveDistinctComposeKeysAcrossAccounts() {
        val firstPerson = person(configId = 41L)
        val secondPerson = person(configId = 42L)
        val firstMemory = memory(configId = 41L)
        val secondMemory = memory(configId = 42L)
        val firstLink = sharedLink(configId = 41L)
        val secondLink = sharedLink(configId = 42L)

        assertNotEquals(firstPerson.accountKey, secondPerson.accountKey)
        assertNotEquals(firstMemory.accountKey, secondMemory.accountKey)
        assertNotEquals(firstLink.accountKey, secondLink.accountKey)
    }

    @Test
    fun personDetailAndViewerRoutePatternsRequireOwningAccount() {
        val detail = Screen.PersonDetailScreen.personId()
        val viewer = Screen.MediaViewScreen.idAndPerson()

        assertTrue(detail.contains("configId={configId}"))
        assertTrue(detail.contains("personId={personId}"))
        assertTrue(viewer.contains("configId={configId}"))
        assertTrue(viewer.contains("personId={personId}"))
    }

    private fun person(configId: Long) = PersonInfo(
        id = "same-id",
        name = "Person",
        providerType = ProviderType.IMMICH,
        serverConfigId = configId
    )

    private fun memory(configId: Long) = MemoryInfo(
        id = "same-id",
        providerType = ProviderType.IMMICH,
        serverConfigId = configId
    )

    private fun sharedLink(configId: Long) = SharedLinkInfo(
        id = "same-id",
        key = "key",
        providerType = ProviderType.IMMICH,
        serverConfigId = configId
    )
}
