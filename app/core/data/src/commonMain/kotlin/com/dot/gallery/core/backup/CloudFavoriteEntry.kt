/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** A cloud favorite, identified by its owning account, provider, and remote id. */
@Serializable
data class CloudFavoriteEntry(
    val providerType: String,
    val remoteId: String,
    /** Stable identity within the source backup. Preferred over the device-local database id. */
    val sourceAccountId: String = "",
    /** Legacy schema-v1 account reference. Never use directly as a destination database id. */
    val serverConfigId: Long = 0L
)
