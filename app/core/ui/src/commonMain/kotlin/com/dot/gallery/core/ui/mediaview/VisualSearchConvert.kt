/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.ui.mediaview

import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.isImage

/**
 * Formats a visual-search provider is expected to decode natively. Anything else (RAW, TIFF, PSD,
 * JP2, JXL, SVG, …) is exotic and goes through the ask/always/never conversion policy.
 */
val VISUAL_SEARCH_SAFE_MIMES = setOf(
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/gif",
    "image/bmp",
    "image/heic",
    "image/heif",
    "image/avif",
)

fun needsVisualSearchConvert(mimeType: String?, isImage: Boolean): Boolean =
    isImage && mimeType?.lowercase() !in VISUAL_SEARCH_SAFE_MIMES

val Media.needsVisualSearchConvert: Boolean
    get() = needsVisualSearchConvert(mimeType, isImage)
