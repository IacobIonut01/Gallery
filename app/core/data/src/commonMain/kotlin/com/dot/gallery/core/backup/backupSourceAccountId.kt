/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** Stable backup-local identity for records owned by a source cloud account. */
fun backupSourceAccountId(providerType: String, sourceConfigId: Long): String =
    "$providerType:$sourceConfigId"
