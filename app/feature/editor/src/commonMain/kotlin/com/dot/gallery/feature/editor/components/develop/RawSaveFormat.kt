/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.editor.components.develop

import org.jetbrains.compose.resources.StringResource
import com.dot.gallery.feature.editor.generated.resources.Res
import com.dot.gallery.feature.editor.generated.resources.raw_export_jpeg
import com.dot.gallery.feature.editor.generated.resources.raw_export_png
import com.dot.gallery.feature.editor.generated.resources.raw_export_tiff16
import com.dot.gallery.feature.editor.generated.resources.raw_export_tiff8


/**
 * Output formats offered when saving a developed RAW from the editor.
 *
 * JPEG/PNG bake the full editor recipe (develop + crop/filters/markup) onto the full-resolution
 * image. TIFF (8/16-bit) is streamed straight from LibRaw at full bit depth and therefore reflects
 * only the develop recipe — so it is offered only when no post-develop editor adjustments exist.
 */
enum class RawSaveFormat(
    val labelRes: StringResource,
    val mimeType: String,
    val ext: String,
    val bits: Int,
    val isTiff: Boolean,
) {
    JPEG(Res.string.raw_export_jpeg, "image/jpeg", "jpg", 8, false),
    PNG(Res.string.raw_export_png, "image/png", "png", 8, false),
    TIFF_8(Res.string.raw_export_tiff8, "image/tiff", "tiff", 8, true),
    TIFF_16(Res.string.raw_export_tiff16, "image/tiff", "tiff", 16, true),
}
