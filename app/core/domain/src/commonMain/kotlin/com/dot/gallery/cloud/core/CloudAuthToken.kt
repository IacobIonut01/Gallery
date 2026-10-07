/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable

@Serializable
data class CloudAuthToken(
    val accessToken: String,
    val userId: String? = null,
    val userEmail: String? = null,
    val isAdmin: Boolean = false
)
