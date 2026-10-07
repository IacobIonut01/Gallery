/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer.components.video

import android.net.Uri
import com.dot.gallery.domain.model.SubtitleTrack
import com.dot.gallery.core.util.or


data class VideoControllerState(
    val subtitleTracks: List<SubtitleTrack>,
    val onSelectSubtitle: (SubtitleTrack) -> Unit,
    val onDisableSubtitles: () -> Unit,
    val onAddExternalSubtitle: (Uri) -> Unit,
    val onRemoveSubtitle: (SubtitleTrack) -> Unit,
    /** Detected HDR format of the selected video track (NONE for SDR or before tracks resolve). */
    val videoHdr: VideoHdrInfo = VideoHdrInfo.NONE,
)
