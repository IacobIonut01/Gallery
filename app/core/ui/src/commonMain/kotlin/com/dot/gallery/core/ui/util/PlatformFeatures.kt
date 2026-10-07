/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.util

/**
 * Whether the shimmer skeleton sweep should animate. The app gates the effect on
 * Android 13+; other platforms always support the pure-draw implementation.
 */
expect val supportsLoadingShimmer: Boolean

/**
 * Whether blur effects (Haze/RenderEffect) are supported. App gates on Android 12+;
 * other platforms support the pure-draw path.
 */
expect val supportsBlur: Boolean
