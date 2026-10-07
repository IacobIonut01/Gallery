/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.date

import android.text.format.DateFormat
import androidx.core.os.ConfigurationCompat
import android.content.res.Resources
import java.util.Locale

private fun currentLocale(): Locale =
    ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0] ?: Locale.getDefault()

actual fun Long.formatEpochSec(pattern: String): String =
    DateFormat.format(pattern, this * 1000L).toString()

actual fun systemDatePattern(skeleton: String): String =
    DateFormat.getBestDateTimePattern(currentLocale(), skeleton)
