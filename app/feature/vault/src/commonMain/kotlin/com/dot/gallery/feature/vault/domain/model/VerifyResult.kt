/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.domain.model

/** Result of a vault credential verification call. */
sealed interface VerifyResult {
    /** Secret matched. */
    data object Success : VerifyResult
    /** Secret did not match. [attemptsLeft] before lockout (0 = just locked out). */
    data class Failed(val attemptsLeft: Int) : VerifyResult
    /** Too many consecutive failures. Try again after [cooldownMs] milliseconds. */
    data class LockedOut(val cooldownMs: Long) : VerifyResult
}
