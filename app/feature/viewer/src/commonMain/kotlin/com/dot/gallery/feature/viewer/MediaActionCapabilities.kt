/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.viewer

/**
 * The complete set of viewer actions allowed for a media item.
 *
 * Action surfaces consume this value rather than duplicating source/read-only/provider checks. This
 * is intentionally immutable and platform-free so the policy matrix can be covered by JVM tests.
 */
data class MediaActionCapabilities(
    val share: Boolean,
    val visualSearch: Boolean,
    val copyToClipboard: Boolean,
    val favorite: Boolean,
    val edit: Boolean,
    val rotate: Boolean,
    val rename: Boolean,
    val trash: Boolean,
    val hideInVault: Boolean,
    val restoreFromVault: Boolean,
    val copyOrMove: Boolean,
    val addToCollection: Boolean,
    val setAlbumCover: Boolean,
    val openExternally: Boolean,
    val download: Boolean,
    val extractFrames: Boolean,
    val exportMotionVideo: Boolean,
    val cutout: Boolean,
)
