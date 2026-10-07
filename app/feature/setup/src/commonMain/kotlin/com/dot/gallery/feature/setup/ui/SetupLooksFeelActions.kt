/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/** Platform hooks for the Looks & Feel page. */
data class SetupLooksFeelActions(
    /** Opens the app's appearance settings screen (app resolves the route). */
    val onOpenAppearance: () -> Unit = {},
)
