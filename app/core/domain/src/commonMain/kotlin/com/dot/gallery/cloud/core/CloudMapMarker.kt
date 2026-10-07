/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable

@Serializable
data class CloudMapMarker(
    val latitude: Double,
    val longitude: Double,
    val assetId: String,
    val providerType: ProviderType,
    val city: String? = null,
    val country: String? = null
)
