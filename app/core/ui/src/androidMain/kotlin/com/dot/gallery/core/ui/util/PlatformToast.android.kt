/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.util

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformToast(): (String) -> Unit {
    val context = LocalContext.current
    return { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
}
