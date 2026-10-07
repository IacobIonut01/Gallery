/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.cast

import android.content.Context
import androidx.lifecycle.ViewModel
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.getUri
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import com.dot.gallery.data.data_source.KeychainHolder
import com.dot.gallery.domain.util.isEncrypted
import com.dot.gallery.domain.uri.path
import com.dot.gallery.domain.util.volume


class FCastViewModel @Inject constructor(
    private val appContext: Context,
    private val session: FCastSession
) : ViewModel() {

    val state: StateFlow<CastSessionState> = session.state

    val isConnected: Boolean get() = session.isConnected
    val isCasting: Boolean get() = session.isCasting

    fun isCastAvailable(): Boolean = session.isCastAvailable()
    fun checkPermissions(): List<CastPermission> = session.checkPermissions()
    fun hasAllPermissions(): Boolean = session.hasAllPermissions()

    fun startDiscovery() = session.startDiscovery()
    fun stopDiscovery() = session.stopDiscovery()

    fun connect(device: FCastDevice) {
        session.stopDiscovery()
        session.connect(device)
    }

    fun disconnect() = session.disconnect()

    fun <T : Media> castMedia(media: T) {
        if (media.isEncrypted) {
            castEncryptedMedia(media)
        } else {
            session.castMedia(media)
        }
    }

    private fun <T : Media> castEncryptedMedia(media: T) {
        val keychainHolder = KeychainHolder(appContext)
        try {
            val encryptedFile = File(media.getUri().path!!)
            val decrypted = keychainHolder.decryptVaultMedia(encryptedFile)
            val tempFile = File.createTempFile("fcast_${media.id}", null, appContext.cacheDir)
            decrypted.openStream().use { input ->
                FileOutputStream(tempFile).use { out ->
                    input.copyTo(out)
                }
            }
            decrypted.cleanup()
            session.castFile(
                file = tempFile,
                mimeType = media.mimeType,
                label = media.label,
                mediaId = media.id
            )
        } catch (e: Exception) {
            // Fallback: try as regular media
            session.castMedia(media)
        }
    }

    fun togglePlayPause() = session.togglePlayPause()
    fun pause() = session.pause()
    fun resume() = session.resume()
    fun seek(timeSeconds: Double) = session.seek(timeSeconds)
    fun setVolume(volume: Double) = session.setVolume(volume)
    fun setSpeed(speed: Double) = session.setSpeed(speed)
    fun stopCasting() = session.stop()
}
