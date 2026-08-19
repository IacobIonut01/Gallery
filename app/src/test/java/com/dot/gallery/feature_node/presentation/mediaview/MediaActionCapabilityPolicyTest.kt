/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview

import com.dot.gallery.cloud.core.CloudAccountRuntimeSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaActionCapabilityPolicyTest {

    @Test
    fun localMediaStoreImageAllowsSupportedMutations() {
        val result = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(
                isImage = true,
                sourceAllowsMutation = true,
                sourceAllowsDelete = true,
                platformSupportsFavorite = true,
            )
        )

        assertTrue(result.share)
        assertTrue(result.favorite)
        assertTrue(result.edit)
        assertTrue(result.rotate)
        assertTrue(result.trash)
        assertTrue(result.hideInVault)
        assertTrue(result.copyOrMove)
        assertTrue(result.addToCollection)
    }

    @Test
    fun cloudReadOnlyAllowsViewingAndDownloadButNoExportOrMutation() {
        val result = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(
                isCloud = true,
                isImage = true,
                sourceAllowsDelete = true,
                cloudReadOnly = true,
                providerSupportsFavorite = true,
                providerSupportsTrash = true,
            )
        )

        assertFalse(result.share)
        assertFalse(result.copyToClipboard)
        assertFalse(result.favorite)
        assertFalse(result.edit)
        assertFalse(result.rotate)
        assertFalse(result.trash)
        assertFalse(result.cutout)
        assertTrue(result.download)
        assertFalse(result.openExternally)
    }

    @Test
    fun writableCloudUsesProviderFavoriteAndTrashCapabilities() {
        val withoutProviderCapabilities = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(
                isCloud = true,
                isImage = true,
                sourceAllowsDelete = true,
            )
        )
        val withProviderCapabilities = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(
                isCloud = true,
                isImage = true,
                sourceAllowsDelete = true,
                providerSupportsFavorite = true,
                providerSupportsTrash = true,
            )
        )

        assertFalse(withoutProviderCapabilities.favorite)
        assertTrue(withoutProviderCapabilities.trash) // permanent delete remains available
        assertTrue(withProviderCapabilities.favorite)
        assertTrue(withProviderCapabilities.trash)
        assertFalse(withProviderCapabilities.edit)
        assertFalse(withProviderCapabilities.rotate)
    }

    @Test
    fun sameProviderAccountsUseReadOnlySettingFromExactUriConfig() {
        val settings = mapOf(
            101L to CloudAccountRuntimeSettings(readOnlyMode = true),
            202L to CloudAccountRuntimeSettings(readOnlyMode = false),
        )

        val first = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(
                isCloud = true,
                cloudReadOnly = cloudMediaReadOnly("cloud://IMMICH/shared-id?cfg=101", settings),
                sourceAllowsDelete = true,
            )
        )
        val second = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(
                isCloud = true,
                cloudReadOnly = cloudMediaReadOnly("cloud://IMMICH/shared-id?cfg=202", settings),
                sourceAllowsDelete = true,
            )
        )

        assertFalse(first.share)
        assertFalse(first.trash)
        assertTrue(second.share)
        assertTrue(second.trash)
    }

    @Test
    fun missingAccountDoesNotBorrowReadOnlySettingFromAnotherAccount() {
        val settings = mapOf(
            101L to CloudAccountRuntimeSettings(readOnlyMode = true),
        )

        assertFalse(cloudMediaReadOnly("cloud://IMMICH/shared-id?cfg=202", settings))
        assertFalse(cloudMediaReadOnly("cloud://IMMICH/shared-id", settings))
    }

    @Test
    fun readUriOnlyKeepsReadActionsButRejectsSourceMutation() {
        val result = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(
                isImage = true,
                isReadUriOnly = true,
                sourceAllowsMutation = false,
            )
        )

        assertTrue(result.share)
        assertTrue(result.copyToClipboard)
        assertTrue(result.openExternally)
        assertFalse(result.favorite)
        assertFalse(result.edit)
        assertFalse(result.rotate)
        assertFalse(result.trash)
    }

    @Test
    fun vaultMediaOnlyExposesVaultMutationsWhenHandlerExists() {
        val unavailable = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(isEncrypted = true)
        )
        val restoreOnly = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(isEncrypted = true, vaultRestoreAvailable = true)
        )
        val available = MediaActionCapabilityPolicy.resolve(
            MediaActionPolicyInput(
                isEncrypted = true,
                vaultRestoreAvailable = true,
                vaultDeleteAvailable = true,
            )
        )

        assertFalse(unavailable.restoreFromVault)
        assertFalse(unavailable.trash)
        assertTrue(restoreOnly.restoreFromVault)
        assertFalse(restoreOnly.trash)
        assertTrue(available.restoreFromVault)
        assertTrue(available.trash)
        assertFalse(available.edit)
        assertFalse(available.rotate)
        assertFalse(available.openExternally)
    }
}
