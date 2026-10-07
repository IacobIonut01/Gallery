/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.vault

import android.net.Uri
import com.dot.gallery.domain.model.Vault
import kotlinx.coroutines.flow.Flow

/**
 * App-provided backend for [VaultViewModel]: the WorkManager vault-operation pipeline stays
 * app-side (workers are DI-bound there) while this module hosts the ViewModel itself.
 */
interface VaultRuntime {

    /** Whether any "VaultOp"-tagged work is currently running. */
    val vaultOpRunning: Flow<Boolean>

    /** 0f..1f aggregate progress across the active "VaultOp"-tagged work. */
    val vaultOpProgress: Flow<Float>

    /** Fire-and-forget enqueue (no observation). */
    suspend fun enqueueVaultOp(
        operation: VaultOp,
        media: List<Uri>,
        vault: Vault?,
        uniqueKey: String? = null,
        deleteOriginals: Boolean = false,
    )

    /**
     * Enqueues the operation and suspends until the work reaches a terminal state, returning
     * its [VaultOpOutcome].
     */
    suspend fun runVaultOp(
        operation: VaultOp,
        media: List<Uri>,
        vault: Vault?,
        uniqueKey: String,
        deleteOriginals: Boolean = false,
    ): VaultOpOutcome
}
