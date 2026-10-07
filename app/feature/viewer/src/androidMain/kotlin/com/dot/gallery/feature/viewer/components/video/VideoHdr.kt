/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer.components.video

import androidx.media3.common.C
import androidx.media3.common.MimeTypes

private val DOLBY_VISION_CODEC_PREFIXES =
    arrayOf("dvhe", "dvh1", "dvav", "dva1", "dav1")

private val DOLBY_VISION_PROFILE_LEVEL_REGEX =
    Regex("""^(?:dvhe|dvh1|dvav|dva1|dav1)\.(\d{1,2})\.(\d{1,2})""", RegexOption.IGNORE_CASE)

/**
 * True when [mimeType]/[codecs] describe a Dolby Vision track, either via the
 * `video/dolby-vision` MIME or via a DV codec string riding on a container MIME.
 */
fun isDolbyVisionCodec(mimeType: String?, codecs: String?): Boolean {
    if (mimeType == MimeTypes.VIDEO_DOLBY_VISION) return true
    val codec = codecs?.trim()?.lowercase() ?: return false
    return DOLBY_VISION_CODEC_PREFIXES.any { codec.startsWith(it) }
}

/**
 * Classify a video track into a [VideoHdrInfo]. [colorTransfer] is the `Format.colorInfo
 * .colorTransfer` value ([C.COLOR_TRANSFER_ST2084], [C.COLOR_TRANSFER_HLG], …) and may be null.
 */
fun classifyVideoHdr(mimeType: String?, codecs: String?, colorTransfer: Int?): VideoHdrInfo {
    if (isDolbyVisionCodec(mimeType, codecs)) {
        val match = codecs?.trim()?.let(DOLBY_VISION_PROFILE_LEVEL_REGEX::find)
        return VideoHdrInfo(
            type = VideoHdrType.DOLBY_VISION,
            dolbyVisionProfile = match?.groupValues?.get(1)?.toIntOrNull(),
            dolbyVisionLevel = match?.groupValues?.get(2)?.toIntOrNull(),
            codecs = codecs,
        )
    }
    return when (colorTransfer) {
        C.COLOR_TRANSFER_ST2084 -> VideoHdrInfo(type = VideoHdrType.HDR10, codecs = codecs)
        C.COLOR_TRANSFER_HLG -> VideoHdrInfo(type = VideoHdrType.HLG, codecs = codecs)
        else -> VideoHdrInfo.NONE
    }
}
