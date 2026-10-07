/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.encryption

data class EncryptionRuntimeState(
    val settings: EncryptionBackendState = EncryptionBackendState.UNKNOWN,
    val database: EncryptionBackendState = EncryptionBackendState.UNKNOWN,
) {
    val hasPlaintextFallback: Boolean
        get() = settings == EncryptionBackendState.PLAINTEXT_FALLBACK ||
            database == EncryptionBackendState.PLAINTEXT_FALLBACK

    val fullyEncrypted: Boolean
        get() = settings == EncryptionBackendState.ENCRYPTED &&
            database == EncryptionBackendState.ENCRYPTED
}
