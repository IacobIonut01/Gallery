/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import kotlinx.serialization.Serializable

@Serializable
data class StartupCacheStamp(
    val mediaVersion: String,
    val permissionMask: Int
)
