/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.util

import androidx.compose.runtime.Composable

/** Returns a lightweight one-shot notifier — `Toast` on Android. */
@Composable
expect fun rememberPlatformToast(): (String) -> Unit
