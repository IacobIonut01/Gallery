/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

fun <T> findUnambiguousCloudAccount(
    target: BackupCloudAccountIdentity,
    existing: List<T>,
    identity: (T) -> BackupCloudAccountIdentity
): T? = existing.filter { identity(it) == target }.singleOrNull()
