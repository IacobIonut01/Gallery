/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.exif.ui

import com.dot.gallery.cloud.core.ProviderType

/** Presentation descriptor of a cloud account shown as a transfer destination section. */
data class CloudDestinationAccount(
    val providerType: ProviderType,
    val title: String,
    val subtitle: String
)
