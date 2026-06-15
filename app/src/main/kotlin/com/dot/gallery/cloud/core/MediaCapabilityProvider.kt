/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

interface MediaCapabilityProvider {
    val providerType: ProviderType
    val displayName: String
    val isAvailable: Boolean
    val capabilities: Set<ProviderCapability>
}
