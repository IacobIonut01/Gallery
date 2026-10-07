/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/**
 * How much of the device media library the app may read, as granted by the system.
 * Mirrors the app-side `MediaAccessState` (`core/presentation/components/util`),
 * which stays Android-bound because it is derived from runtime permission checks.
 */
enum class SetupMediaAccess {
    NONE,
    LIMITED,
    FULL,
}
