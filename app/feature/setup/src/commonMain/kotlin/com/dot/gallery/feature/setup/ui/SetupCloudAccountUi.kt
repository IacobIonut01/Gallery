/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/** A cloud account already connected during setup, shown as a configured card. */
data class SetupCloudAccountUi(
    val providerName: String,
    val serverUrl: String,
)
