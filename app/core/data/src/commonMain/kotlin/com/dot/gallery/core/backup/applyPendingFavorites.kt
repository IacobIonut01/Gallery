/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

suspend fun applyPendingFavorites(
    pending: Set<PendingCloudFavorite>,
    providerType: String,
    serverConfigId: Long,
    apply: suspend (PendingCloudFavorite) -> Boolean
): PendingFavoriteApplyResult {
    val applicable = pending.filter {
        it.providerType == providerType && it.serverConfigId == serverConfigId
    }
    val applied = applicable.filterTo(mutableSetOf()) { apply(it) }
    return PendingFavoriteApplyResult(applied = applied, remaining = pending - applied)
}
