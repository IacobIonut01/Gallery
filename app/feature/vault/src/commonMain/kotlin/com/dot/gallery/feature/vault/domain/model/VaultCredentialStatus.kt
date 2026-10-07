/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.domain.model

/** Whether stored custom authentication can be safely challenged. */
sealed interface VaultCredentialStatus {
    data object Missing : VaultCredentialStatus
    data object Corrupt : VaultCredentialStatus
    data class Valid(val authType: VaultAuthType) : VaultCredentialStatus
}
