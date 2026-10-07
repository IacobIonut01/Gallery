/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import kotlin.math.pow

/** TIFF output compression for export, mapped to the JNI `compression` arg. */
enum class RawTiffCompression(val nativeValue: Int) { NONE(0), DEFLATE(1), LZW(2) }