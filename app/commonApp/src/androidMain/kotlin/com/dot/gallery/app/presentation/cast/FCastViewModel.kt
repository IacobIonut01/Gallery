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
) : ViewModel(), MediaViewerCastController {

    override val state: StateFlow<CastSessionState> = session.state

    val isConnected: Boolean get() = session.isConnected
    val isCasting: Boolean get() = session.isCasting

    override fun isCastAvailable(): Boolean = session.isCastAvailable()
    override fun checkPermissions(): List<CastPermission> = session.checkPermissions()
    override fun hasAllPermissions(): Boolean = session.hasAllPermissions()

    override fun startDiscovery() = session.startDiscovery()
    override fun stopDiscovery() = session.stopDiscovery()

    override fun connect(device: FCastDevice) {
        session.stopDiscovery()
        session.connect(device)
    }

    override fun disconnect() = session.disconnect()

    override fun <T : Media> castMedia(media: T) {
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
    override fun pause() = session.pause()
    override fun resume() = session.resume()
    override fun seek(timeSeconds: Double) = session.seek(timeSeconds)
    override fun setVolume(volume: Double) = session.setVolume(volume)
    override fun setSpeed(speed: Double) = session.setSpeed(speed)
    override fun stopCasting() = session.stop()
}
