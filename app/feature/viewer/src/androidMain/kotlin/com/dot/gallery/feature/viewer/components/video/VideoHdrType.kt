/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer.components.video

/**
 * HDR classification of a video track, derived from the track's declared MIME, codec string
 * and color transfer — no device state involved, so it is fully unit-testable.
 */
enum class VideoHdrType {
    NONE,

    /** PQ / BT.2020 / ST2084 — covers HDR10; HDR10+ SEI metadata is not exposed at this layer. */
    HDR10,

    /** Hybrid Log-Gamma. */
    HLG,

    /** Dolby Vision (dvhe/dvh1/dvav/dva1/dav1 codec strings or the video/dolby-vision MIME). */
    DOLBY_VISION,
}
