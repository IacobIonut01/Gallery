/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.vault

import android.net.Uri
import com.dot.gallery.domain.model.Vault
import kotlinx.coroutines.flow.Flow

/** Terminal outcome of a vault operation work item. */
sealed interface VaultOpOutcome {
    /**
     * [succeededCount] items landed in the vault — null when the worker did not report a
     * count (older data); [leftoverUris] failed post-ops (e.g. original deletion).
     */
    data class Succeeded(val succeededCount: Int?, val leftoverUris: List<Uri>) : VaultOpOutcome
    data object Failed : VaultOpOutcome
    /** Finished but neither succeeded nor failed (cancelled/blocked). */
    data object Cancelled : VaultOpOutcome
}