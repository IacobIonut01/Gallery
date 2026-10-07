/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.dot.gallery.domain.model.UriMedia

@Serializable
data class MemoryInfo(
    val id: String,
    val type: String = "on_this_day",
    val year: Int = 0,
    val assetCount: Int = 0,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val createdAt: Long = 0L,
    val seenAt: Long? = null,
    @Transient
    val media: List<UriMedia> = emptyList()
) {
    val accountKey: String
        get() = "${providerType.name}/$serverConfigId/$id"
}
