/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.domain.settings

import com.dot.gallery.domain.settings.PrefKey

/**
 * Feature-local preference keys for the setup wizard, mirroring the app's
 * `Settings.Misc` entries (the app-side `Settings.kt` stays the legacy reader for
 * startup gating — same key names and defaults keep both in sync).
 */
object SetupKeys {

    /**
     * Version of the first-launch setup wizard the user last completed. Bump
     * [CURRENT_SETUP_VERSION] whenever the wizard is reworked to force every existing
     * user through it again. Mirrors `Settings.Misc.CURRENT_SETUP_VERSION`.
     */
    const val CURRENT_SETUP_VERSION = 1

    /** Mirrors `Settings.Misc.SETUP_COMPLETED_VERSION` (`intPreferencesKey`, default 0). */
    val setupCompletedVersion = PrefKey.int("setup_completed_version", default = 0)
}
