/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.encryption

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Runtime source of truth for what the app actually opened, not what it intended to open. */
object EncryptionStateMonitor {
    private val _state = MutableStateFlow(EncryptionRuntimeState())
    val state: StateFlow<EncryptionRuntimeState> = _state.asStateFlow()

    fun reportSettings(state: EncryptionBackendState) {
        _state.update { it.copy(settings = state) }
    }

    fun reportDatabase(state: EncryptionBackendState) {
        _state.update { it.copy(database = state) }
    }
}
