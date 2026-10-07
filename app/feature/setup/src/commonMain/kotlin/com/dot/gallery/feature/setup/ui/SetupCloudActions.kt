/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

import com.dot.gallery.cloud.core.ProviderType

/** Platform hooks for the cloud page. */
data class SetupCloudActions(
    /** Starts the add-server flow for the picked provider (app resolves the route). */
    val onSetupProvider: (ProviderType) -> Unit = {},
)
