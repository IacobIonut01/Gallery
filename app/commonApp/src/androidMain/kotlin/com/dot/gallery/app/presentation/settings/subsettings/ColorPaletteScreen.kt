/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.settings.subsettings

import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.feature.settings.ui.ColorPaletteContent
import com.dot.gallery.feature.settings.ui.ColorPaletteOption
import com.dot.gallery.feature.settings.ui.ColorPaletteState
import com.dot.gallery.core.presentation.util.shouldShowManualCartoKeySetting
import com.dot.gallery.ui.theme.isDarkTheme
import com.dot.gallery.BuildConfig

/**
 * Android shell for [ColorPaletteContent]: supplies flavor flags, SDK gating and
 * the dynamic (wallpaper) palette options, which only exist on Android 12+.
 */
@Composable
fun ColorPaletteScreen() {
    val context = LocalContext.current
    val isDark = isDarkTheme()

    // Extract wallpaper color variations from the dynamic color scheme
    val wallpaperPalettes = remember(isDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dynamicScheme = if (isDark) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            listOf(
                dynamicScheme.primary,
                dynamicScheme.secondary,
                dynamicScheme.tertiary
            ).mapIndexed { index, color ->
                val argb = color.toArgb()
                val hex = String.format("%08X", argb)
                ColorPaletteOption(
                    name = when (index) {
                        0 -> "Primary"
                        1 -> "Secondary"
                        else -> "Tertiary"
                    },
                    seedArgb = argb,
                    hexKey = hex
                )
            }.distinctBy { it.hexKey }
        } else {
            emptyList()
        }
    }

    ColorPaletteContent(
        state = ColorPaletteState(
            supportsBlur = SdkCompat.supportsBlur,
            mapsEnabled = BuildConfig.MAPS_ENABLED,
            showManualCartoKey = shouldShowManualCartoKeySetting(
                mapsEnabled = BuildConfig.MAPS_ENABLED,
                embeddedKey = BuildConfig.CARTO_BASEMAP_KEY,
            ),
            wallpaperPalettes = wallpaperPalettes,
        )
    )
}
