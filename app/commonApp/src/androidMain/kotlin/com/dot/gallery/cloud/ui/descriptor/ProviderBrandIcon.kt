/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.descriptor

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import com.dot.gallery.cloud.core.ProviderType


/**
 * Renders the branding for a [ProviderType], driven entirely by its [ProviderUiDescriptor].
 *
 * - When the descriptor declares a brand [ProviderUiDescriptor.iconRes] drawable it is used;
 *   monochrome marks ([ProviderUiDescriptor.iconTinted] = true) are tinted with [tint], while
 *   full-color logos (e.g. Azure) render at their native colors.
 * - Otherwise the descriptor's fallback Material [ProviderUiDescriptor.icon] vector is drawn.
 */
@Composable
fun ProviderBrandIcon(
    providerType: ProviderType,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val descriptor = ProviderUiDescriptors.forType(providerType)
    val iconRes = descriptor.iconRes
    when {
        iconRes != null && descriptor.iconTinted -> Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = modifier,
            tint = tint
        )

        iconRes != null -> Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = modifier
        )

        else -> Icon(
            imageVector = descriptor.icon,
            contentDescription = null,
            modifier = modifier,
            tint = tint
        )
    }
}
