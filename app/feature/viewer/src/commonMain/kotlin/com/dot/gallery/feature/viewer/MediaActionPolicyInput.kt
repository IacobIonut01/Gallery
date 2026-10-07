/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

/** Inputs that describe the source and provider guarantees for one viewer item. */
data class MediaActionPolicyInput(
    val isCloud: Boolean = false,
    val isEncrypted: Boolean = false,
    val isImage: Boolean = false,
    val isVideo: Boolean = false,
    val isReadUriOnly: Boolean = false,
    val sourceAllowsMutation: Boolean = false,
    val sourceAllowsDelete: Boolean = sourceAllowsMutation,
    val cloudReadOnly: Boolean = false,
    val providerSupportsFavorite: Boolean = false,
    val providerSupportsTrash: Boolean = false,
    val platformSupportsFavorite: Boolean = false,
    val vaultRestoreAvailable: Boolean = false,
    val vaultDeleteAvailable: Boolean = false,
)
