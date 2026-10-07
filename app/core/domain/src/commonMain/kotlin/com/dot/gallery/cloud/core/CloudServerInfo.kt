/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable

@Serializable
data class CloudServerInfo(
    val version: String,
    val serverName: String,
    val storageUsed: Long = 0L,
    val storageTotal: Long = 0L
)
