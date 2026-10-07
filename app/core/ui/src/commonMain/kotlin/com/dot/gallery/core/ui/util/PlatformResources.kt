/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Resolves an Android `R.drawable` resource id to a [Painter]. Only meaningful
 * on Android — other platforms return a transparent painter until the drawable
 * set is migrated to Compose resources.
 */
@Composable
expect fun rememberPlatformResourcePainter(resId: Int): Painter
