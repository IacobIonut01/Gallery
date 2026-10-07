/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.core.encryption.EncryptionRuntimeState

/**
 * Runtime-injected state for [SettingsSecurityContent]. The three security
 * preferences themselves are read inside the composable via `rememberPreference`.
 *
 * When [advancedProtection] (Android Advanced Protection Mode) is on, sandboxed
 * decode is forced on and "shared" metadata isolation is raised to "hybrid" —
 * the stored preferences stay untouched.
 */
@Stable
data class SettingsSecurityState(
    val advancedProtection: Boolean = false,
    val encryptionState: EncryptionRuntimeState = EncryptionRuntimeState(),
)
