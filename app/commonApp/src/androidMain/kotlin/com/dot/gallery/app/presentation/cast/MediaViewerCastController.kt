/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.cast

import com.dot.gallery.domain.model.Media
import kotlinx.coroutines.flow.StateFlow

/**
 * The cast surface [com.dot.gallery.app.presentation.mediaview.MediaViewScreen]
 * actually consumes. [FCastViewModel] is the production implementation; docs
 * scenes substitute a local no-network controller so the viewer never touches
 * NSD/sockets during captures.
 */
interface MediaViewerCastController {
    val state: StateFlow<CastSessionState>

    fun isCastAvailable(): Boolean
    fun checkPermissions(): List<CastPermission>
    fun hasAllPermissions(): Boolean

    fun startDiscovery()
    fun stopDiscovery()

    fun connect(device: FCastDevice)
    fun disconnect()

    fun <T : Media> castMedia(media: T)

    fun pause()
    fun resume()
    fun seek(timeSeconds: Double)
    fun setVolume(volume: Double)
    fun setSpeed(speed: Double)
    fun stopCasting()
}
