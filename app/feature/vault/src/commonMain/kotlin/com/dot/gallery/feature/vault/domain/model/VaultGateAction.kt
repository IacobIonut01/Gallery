/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.domain.model

/** What the selector gate demands before a vault list may be shown. */
sealed interface VaultGateAction {
    /** Vault names may be revealed. */
    data object Reveal : VaultGateAction

    /** Device biometric/credential authentication must succeed first. */
    data object ChallengeDevice : VaultGateAction

    /** The stored selector credential must be entered first. */
    data class ChallengeCustom(val authType: VaultAuthType) : VaultGateAction

    /** The configured challenge can no longer be satisfied — keep names hidden. */
    data object Deny : VaultGateAction
}
