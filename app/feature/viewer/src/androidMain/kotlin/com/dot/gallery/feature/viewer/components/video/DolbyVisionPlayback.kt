/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer.components.video

/**
 * How Dolby Vision content actually renders on this device, given the detected track and the
 * device's decoder/display capabilities.
 */
enum class DolbyVisionPlayback {
    /** DV decoder + DV display: true Dolby Vision presentation. */
    NATIVE,

    /** DV decoder present but the display is HDR-not-DV: decoder tone-maps the DV output. */
    DECODE_ONLY,

    /** No DV decoder; Media3 falls back to the backward-compatible base layer (HEVC/AVC/AV1). */
    BASE_LAYER,

    /**
     * No DV decoder and no compatible base layer (Dolby Vision profile 5). The HEVC decode
     * produces wrong colors; nothing the app can do about it — label it honestly.
     */
    UNSUPPORTED,
}

/**
 * Combine the track's [VideoHdrInfo] with device capabilities into the effective playback mode.
 * Returns null for non-DV content.
 */
fun dolbyVisionPlayback(
    info: VideoHdrInfo,
    hasDolbyVisionDecoder: Boolean,
    hasDolbyVisionDisplay: Boolean,
): DolbyVisionPlayback? {
    if (info.type != VideoHdrType.DOLBY_VISION) return null
    return when {
        hasDolbyVisionDecoder && hasDolbyVisionDisplay -> DolbyVisionPlayback.NATIVE
        hasDolbyVisionDecoder -> DolbyVisionPlayback.DECODE_ONLY
        info.hasCompatibleDolbyVisionBaseLayer() -> DolbyVisionPlayback.BASE_LAYER
        else -> DolbyVisionPlayback.UNSUPPORTED
    }
}
