/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fixed-size device presets the docs site can switch between. */
enum class DevicePreset(val width: Dp, val height: Dp, val corner: Dp) {
    PHONE(412.dp, 892.dp, 24.dp),
    FOLDABLE(768.dp, 932.dp, 18.dp),
    TABLET(840.dp, 1180.dp, 28.dp),
    FULL(0.dp, 0.dp, 0.dp);

    companion object {
        fun parse(value: String?): DevicePreset = when (value?.lowercase()) {
            "tablet" -> TABLET
            "foldable" -> FOLDABLE
            "full", "none" -> FULL
            else -> PHONE
        }
    }
}

/**
 * Renders [content] inside a fixed-size clipped surface emulating a device
 * frame, centered on the page background.
 */
@Composable
fun DeviceStage(
    device: DevicePreset,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pageColor = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageColor),
        contentAlignment = Alignment.Center,
    ) {
        if (device == DevicePreset.FULL) {
            Box(Modifier.fillMaxSize()) { content() }
        } else {
            val shape = RoundedCornerShape(device.corner)
            Box(
                modifier = Modifier
                    .size(device.width, device.height)
                    .shadow(24.dp, shape)
                    .clip(shape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                content()
            }
        }
    }
}
