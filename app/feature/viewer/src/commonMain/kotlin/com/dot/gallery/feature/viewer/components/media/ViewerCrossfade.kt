/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer.components.media

/**
 * Milliseconds for a full-image cross-fade, or null when the transition must be off.
 *
 * Sketch's cross-fade factory rejects a duration of 0. Item animation off therefore disables the
 * transition instead of asking for a zero-length one, which is what used to crash the viewer on
 * an animated or encrypted item.
 */
fun fullImageCrossfadeMillis(animationsEnabled: Boolean): Int? =
    if (animationsEnabled) 200 else null
