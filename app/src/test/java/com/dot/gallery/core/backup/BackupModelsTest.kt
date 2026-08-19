/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun currentAndLegacySchemasAreSupportedButFutureSchemaIsRejected() {
        BackupManifest.requireSupportedSchema(1)
        BackupManifest.requireSupportedSchema(BackupManifest.SCHEMA_VERSION)

        val error = assertThrows(UnsupportedBackupSchemaException::class.java) {
            BackupManifest.requireSupportedSchema(BackupManifest.SCHEMA_VERSION + 1)
        }
        assertEquals(BackupManifest.SCHEMA_VERSION + 1, error.schemaVersion)
    }

    @Test
    fun schemaOneConfigDefaultsToReauthenticationAndDoesNotInventSourceIdentity() {
        val manifest = json.decodeFromString<BackupManifest>(
            """
            {
              "schemaVersion": 1,
              "cloudConfigs": [{
                "providerType": "IMMICH",
                "serverUrl": "https://photos.example",
                "apiKey": "device-bound-ciphertext"
              }]
            }
            """.trimIndent()
        )

        val config = manifest.cloudConfigs.single()
        assertTrue(config.requiresReauthentication)
        assertEquals(0L, config.sourceConfigId)
        assertEquals("", config.sourceAccountId)
    }

    @Test
    fun accountMappingPrefersSourceIdentityAndOnlyUsesUnambiguousProviderFallback() {
        val mappings = BackupAccountMappings(
            bySourceAccount = mapOf("IMMICH:42" to 900L),
            destinationIdsByProvider = mapOf(
                "IMMICH" to setOf(800L, 900L),
                "OWNCLOUD" to setOf(700L)
            )
        )

        assertEquals(900L, mappings.resolveDestination("IMMICH:42", "IMMICH"))
        assertEquals(700L, mappings.resolveDestination("", "OWNCLOUD"))
        assertEquals(null, mappings.resolveDestination("", "IMMICH"))
    }

    @Test
    fun schemaTwoRecordsShareStableBackupLocalAccountIdentity() {
        val sourceAccountId = backupSourceAccountId("IMMICH", 42L)
        val manifest = BackupManifest(
            cloudConfigs = listOf(
                CloudConfigEntry(
                    providerType = "IMMICH",
                    serverUrl = "https://photos.example",
                    sourceConfigId = 42L,
                    sourceAccountId = sourceAccountId,
                    requiresReauthentication = true
                )
            ),
            cloudFavorites = listOf(
                CloudFavoriteEntry(
                    providerType = "IMMICH",
                    remoteId = "asset-1",
                    sourceAccountId = sourceAccountId,
                    serverConfigId = 42L
                )
            )
        )

        assertEquals(
            manifest.cloudConfigs.single().sourceAccountId,
            manifest.cloudFavorites.single().sourceAccountId
        )
        assertFalse(manifest.cloudConfigs.single().sourceAccountId.isBlank())
    }
}
