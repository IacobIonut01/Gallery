/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.vault.ui.components

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue

/**
 * Bridge to the platform device-credential prompt (BiometricPrompt on Android).
 *
 * The app shell owns the prompt lifetime and writes the one-shot result into the
 * backing state handed to this bridge; shared vault UI consumes it through
 * [outcome] and clears it with [consumeOutcome].
 */
@Stable
class VaultDeviceAuthBridge(
    val isSupported: Boolean,
    private val authenticateAction: () -> Unit,
    private val cancelAction: () -> Unit,
    private val outcomeState: MutableState<Boolean?>,
) {
    /** One-shot prompt result: `null` = idle, `true` = success, `false` = failed/cancelled. */
    val outcome: Boolean? get() = outcomeState.value

    fun authenticate() {
        outcomeState.value = null
        authenticateAction()
    }

    fun cancel() {
        outcomeState.value = null
        cancelAction()
    }

    fun consumeOutcome() {
        outcomeState.value = null
    }
}
