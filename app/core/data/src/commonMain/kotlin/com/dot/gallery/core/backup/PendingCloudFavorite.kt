/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** A cloud favorite awaiting the first indexed row for its destination account. */
@Serializable
data class PendingCloudFavorite(
    val providerType: String,
    val serverConfigId: Long,
    val remoteId: String
)
