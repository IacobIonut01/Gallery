/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import com.dot.gallery.core.ui.generated.resources.Res
import com.dot.gallery.core.ui.generated.resources.google_sans_bold
import com.dot.gallery.core.ui.generated.resources.google_sans_bold_italic
import com.dot.gallery.core.ui.generated.resources.google_sans_italic
import com.dot.gallery.core.ui.generated.resources.google_sans_medium
import com.dot.gallery.core.ui.generated.resources.google_sans_medium_italic
import com.dot.gallery.core.ui.generated.resources.google_sans_regular
import com.dot.gallery.core.ui.generated.resources.google_sans_semibold

val GoogleSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.google_sans_regular, FontWeight.Normal),
        Font(Res.font.google_sans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.google_sans_medium, FontWeight.Medium),
        Font(Res.font.google_sans_medium_italic, FontWeight.Medium, FontStyle.Italic),
        Font(Res.font.google_sans_semibold, FontWeight.SemiBold),
        Font(Res.font.google_sans_bold, FontWeight.Bold),
        Font(Res.font.google_sans_bold_italic, FontWeight.Bold, FontStyle.Italic),
    )

private val defaultTypography = Typography()

val Typography: Typography
    @Composable
    get() = Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = GoogleSansFontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = GoogleSansFontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = GoogleSansFontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = GoogleSansFontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = GoogleSansFontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = GoogleSansFontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = GoogleSansFontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = GoogleSansFontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = GoogleSansFontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = GoogleSansFontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = GoogleSansFontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = GoogleSansFontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = GoogleSansFontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = GoogleSansFontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = GoogleSansFontFamily),
    )

val SystemTypography = Typography()
