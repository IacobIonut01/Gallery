/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import com.dot.gallery.app.presentation.cast.CastPermission
import com.dot.gallery.app.presentation.cast.CastSessionState
import com.dot.gallery.app.presentation.cast.FCastDevice
import com.dot.gallery.app.presentation.cast.MediaViewerCastController
import com.dot.gallery.domain.model.Media
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Local, no-network [MediaViewerCastController] for docs captures. State is a
 * MutableStateFlow the scene poses directly; discovery/connect/cast methods only
 * move that in-memory state — no NSD, no sockets, no HTTP server.
 */
class DocsCastController(
    initialState: CastSessionState = CastSessionState(),
) : MediaViewerCastController {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<CastSessionState> = _state

    override fun isCastAvailable(): Boolean = true
    override fun checkPermissions(): List<CastPermission> = emptyList()
    override fun hasAllPermissions(): Boolean = true

    override fun startDiscovery() {
        _state.value = _state.value.copy(isDiscovering = true)
    }

    override fun stopDiscovery() {
        _state.value = _state.value.copy(isDiscovering = false)
    }

    override fun connect(device: FCastDevice) {
        _state.value = _state.value.copy(
            isDiscovering = false,
            isConnecting = false,
            connectedDevice = device,
        )
    }

    override fun disconnect() {
        _state.value = _state.value.copy(connectedDevice = null, castingMediaId = null)
    }

    override fun <T : Media> castMedia(media: T) {
        _state.value = _state.value.copy(castingMediaId = media.id)
    }

    override fun pause() {}
    override fun resume() {}
    override fun seek(timeSeconds: Double) {}
    override fun setVolume(volume: Double) {}
    override fun setSpeed(speed: Double) {}

    override fun stopCasting() {
        _state.value = _state.value.copy(castingMediaId = null)
    }
}
