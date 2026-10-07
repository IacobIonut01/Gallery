/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** Source-account to destination-database mapping built during import. */
data class BackupAccountMappings(
    val bySourceAccount: Map<String, Long> = emptyMap(),
    val destinationIdsByProvider: Map<String, Set<Long>> = emptyMap()
) {
    fun mergedWith(other: BackupAccountMappings): BackupAccountMappings = BackupAccountMappings(
        bySourceAccount = other.bySourceAccount + bySourceAccount,
        destinationIdsByProvider = (destinationIdsByProvider.keys + other.destinationIdsByProvider.keys)
            .associateWith { provider ->
                destinationIdsByProvider[provider].orEmpty() +
                    other.destinationIdsByProvider[provider].orEmpty()
            }
    )

    fun resolveDestination(sourceAccountId: String, providerType: String): Long? =
        bySourceAccount[sourceAccountId] ?: destinationIdsByProvider[providerType]?.singleOrNull()
}
