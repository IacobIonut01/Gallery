/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable

@Serializable
data class PersonInfo(
    val id: String,
    val name: String,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val thumbnailUrl: String? = null,
    val assetCount: Int = 0,
    val birthDate: String? = null,
    /** Hidden people are excluded from list providers; only person-scoped lookups carry this. */
    val hidden: Boolean = false
) {
    val accountKey: String
        get() = "${providerType.name}/$serverConfigId/$id"
}
