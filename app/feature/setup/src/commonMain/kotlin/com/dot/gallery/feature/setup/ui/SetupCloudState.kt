/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

/** Display state for the cloud page: already-connected accounts + selectable providers. */
data class SetupCloudState(
    val accounts: List<SetupCloudAccountUi> = emptyList(),
    val providers: List<SetupProviderUi> = emptyList(),
)
