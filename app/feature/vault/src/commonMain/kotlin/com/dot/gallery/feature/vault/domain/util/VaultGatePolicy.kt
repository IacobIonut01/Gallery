/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.domain.util

import com.dot.gallery.feature.vault.domain.model.GateMode
import com.dot.gallery.feature.vault.domain.model.VaultCredentialStatus
import com.dot.gallery.feature.vault.domain.model.VaultGateAction

/**
 * Resolves the configured [GateMode] into a concrete [VaultGateAction]. This value is
 * intentionally immutable and platform-free so the gate matrix can be covered by JVM tests.
 * A gate whose challenge cannot be presented anymore (device security removed, stored
 * credential missing or corrupt) fails closed rather than exposing the vault list.
 */
object VaultGatePolicy {
    fun resolve(
        mode: GateMode,
        deviceAuthSupported: Boolean,
        credential: VaultCredentialStatus?
    ): VaultGateAction = when (mode) {
        GateMode.NONE -> VaultGateAction.Reveal
        GateMode.DEVICE ->
            if (deviceAuthSupported) VaultGateAction.ChallengeDevice else VaultGateAction.Deny
        GateMode.CUSTOM -> when (credential) {
            is VaultCredentialStatus.Valid -> VaultGateAction.ChallengeCustom(credential.authType)
            VaultCredentialStatus.Missing,
            VaultCredentialStatus.Corrupt,
            null -> VaultGateAction.Deny
        }
    }

    /**
     * Whether a satisfied gate is allowed to cover vault entry too, so one lock
     * protects every vault. Only a real gate (device or custom) can act as the
     * single lock — with no gate configured, each vault keeps its own challenge
     * rather than silently opening unprotected.
     */
    fun shouldSkipVaultAuth(mode: GateMode, lockAllVaults: Boolean): Boolean =
        lockAllVaults && mode != GateMode.NONE
}
