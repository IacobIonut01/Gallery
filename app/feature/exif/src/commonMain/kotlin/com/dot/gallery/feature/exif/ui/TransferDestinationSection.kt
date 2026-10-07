/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.uri.scheme

data class TransferDestinationSection(
    val key: String,
    val title: String,
    val subtitle: String,
    val providerType: ProviderType?,
    val unavailable: Boolean,
    val albums: List<Album>
)

fun buildTransferDestinationSections(
    albums: List<Album>,
    accounts: Map<Long, CloudDestinationAccount>,
    localTitle: String,
    unavailableTitle: String
): List<TransferDestinationSection> {
    val local = albums.filter {
        it.cloudIdentity == null && it.uri.scheme != "cloud" && !it.relativePath.startsWith("cloud/")
    }
    val cloud = albums.mapNotNull { album -> album.cloudIdentity?.let { it to album } }
        .groupBy { it.first.serverConfigId }
    val unavailable = albums.filter {
        it.cloudIdentity == null && (it.uri.scheme == "cloud" || it.relativePath.startsWith("cloud/"))
    }
    return buildList {
        if (local.isNotEmpty()) add(
            TransferDestinationSection(
                key = "local",
                title = localTitle,
                subtitle = "",
                providerType = null,
                unavailable = false,
                albums = local
            )
        )
        cloud.entries.sortedBy {
            "${accounts[it.key]?.providerType?.displayName.orEmpty()}/" +
                "${accounts[it.key]?.title.orEmpty()}/${it.key}"
        }.forEach { (configId, entries) ->
            val account = accounts[configId]
            val providerType = entries.first().first.providerType
            add(
                TransferDestinationSection(
                    key = "cloud-$configId",
                    title = account?.title ?: providerType.displayName,
                    subtitle = account?.subtitle?.takeIf(String::isNotBlank)
                        ?: providerType.displayName,
                    providerType = providerType,
                    unavailable = false,
                    albums = entries.map { it.second }
                )
            )
        }
        if (unavailable.isNotEmpty()) add(
            TransferDestinationSection(
                key = "unavailable-cloud",
                title = unavailableTitle,
                subtitle = "",
                providerType = null,
                unavailable = true,
                albums = unavailable
            )
        )
    }
}
