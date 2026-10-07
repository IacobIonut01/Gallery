/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer.components.video

/**
 * Detected HDR info for the currently selected video track.
 *
 * @param dolbyVisionProfile parsed from the codec string (`dvh1.08.09` → 8) when present.
 * @param dolbyVisionLevel parsed level (`dvh1.08.09` → 9) when present.
 * @param codecs the raw codec string from the track format, for display/debugging.
 * @param decoderName the decoder actually initialized for playback (filled after decoder init).
 */
data class VideoHdrInfo(
    val type: VideoHdrType = VideoHdrType.NONE,
    val dolbyVisionProfile: Int? = null,
    val dolbyVisionLevel: Int? = null,
    val codecs: String? = null,
    val decoderName: String? = null,
) {
    val isHdr: Boolean get() = type != VideoHdrType.NONE

    companion object {
        val NONE = VideoHdrInfo()
    }
}

/**
 * Whether a Dolby Vision stream has a backward-compatible base layer a plain decoder can render.
 * Profile 5 (the single-layer IPTPQc2 profile) is the well-known exception — without a DV
 * decoder it decodes with wrong (purple/green) colors.
 */
fun VideoHdrInfo.hasCompatibleDolbyVisionBaseLayer(): Boolean =
    type != VideoHdrType.DOLBY_VISION || dolbyVisionProfile != 5
