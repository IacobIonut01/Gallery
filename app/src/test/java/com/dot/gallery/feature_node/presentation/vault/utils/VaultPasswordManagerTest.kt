package com.dot.gallery.feature_node.presentation.vault.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultPasswordManagerTest {

    private val salt = "00".repeat(16)
    private val hash = "11".repeat(32)

    @Test
    fun missingCredentialFailsClosed() {
        assertEquals(
            VaultCredentialStatus.Missing,
            VaultPasswordManager.credentialStatusForStoredValue(null)
        )
    }

    @Test
    fun credentialWithoutAuthTypeIsCorrupt() {
        assertEquals(
            VaultCredentialStatus.Corrupt,
            VaultPasswordManager.credentialStatusForStoredValue("$salt:$hash")
        )
    }

    @Test
    fun credentialWithUnknownAuthTypeIsCorrupt() {
        assertEquals(
            VaultCredentialStatus.Corrupt,
            VaultPasswordManager.credentialStatusForStoredValue("UNKNOWN:pbkdf2:$salt:$hash")
        )
    }

    @Test
    fun credentialWithMalformedHexIsCorrupt() {
        assertEquals(
            VaultCredentialStatus.Corrupt,
            VaultPasswordManager.credentialStatusForStoredValue("PIN:pbkdf2:not-hex:$hash")
        )
    }

    @Test
    fun validPbkdf2CredentialExposesItsAuthType() {
        assertEquals(
            VaultCredentialStatus.Valid(VaultAuthType.PIN),
            VaultPasswordManager.credentialStatusForStoredValue("PIN:pbkdf2:$salt:$hash")
        )
    }

    @Test
    fun validTypedLegacyCredentialRemainsSupported() {
        assertEquals(
            VaultCredentialStatus.Valid(VaultAuthType.PASSWORD),
            VaultPasswordManager.credentialStatusForStoredValue("PASSWORD:$salt:$hash")
        )
    }
}
