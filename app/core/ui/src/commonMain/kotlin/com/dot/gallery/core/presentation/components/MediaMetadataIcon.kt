/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Exposure
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Panorama
import androidx.compose.material.icons.outlined.PanoramaPhotosphere
import androidx.compose.ui.graphics.vector.ImageVector
import com.dot.gallery.data.entity.MediaMetadata

fun MediaMetadata.getIcon(): ImageVector? {
    return if (isNightMode) {
        Icons.Outlined.NightsStay
    } else if (isPanorama) {
        Icons.Outlined.Panorama
    } else if (isPhotosphere) {
        Icons.Outlined.PanoramaPhotosphere
    } else if (isLongExposure) {
        Icons.Outlined.Exposure
    } else if (isMotionPhoto) {
        Icons.Outlined.MotionPhotosOn
    } else {
        null
    }
}
