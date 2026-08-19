/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.cloud.core.CredentialEncryptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialEncryptorTest {

    private val encryptor = CredentialEncryptor()

    @Test
    fun roundTripReturnsPlaintext() {
        val encrypted = encryptor.encrypt("secret")

        assertEquals("secret", encryptor.decrypt(encrypted))
    }

    @Test
    fun undecryptableValueIsNeverReturnedAsPlaintext() {
        val invalid = "device-bound-or-corrupted-ciphertext"

        assertNull(encryptor.decryptOrNull(invalid))
        assertEquals("", encryptor.decrypt(invalid))
    }
}
