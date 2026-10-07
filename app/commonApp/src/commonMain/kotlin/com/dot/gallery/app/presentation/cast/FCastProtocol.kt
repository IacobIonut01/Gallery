/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.cast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.dot.gallery.domain.util.volume


/**
 * FCast protocol implementation (v1–v2).
 * TCP on port 46899. Header: uint32 size (LE) + uint8 opcode. Body: UTF-8 JSON.
 */
object FCastProtocol {
    const val DEFAULT_PORT = 46899
    const val SERVICE_TYPE = "_fcast._tcp."
    const val MAX_PACKET_SIZE = 32_000

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }
}

enum class Opcode(val value: Int) {
    PLAY(1),
    PAUSE(2),
    RESUME(3),
    STOP(4),
    SEEK(5),
    PLAYBACK_UPDATE(6),
    VOLUME_UPDATE(7),
    SET_VOLUME(8),
    PLAYBACK_ERROR(9),
    SET_SPEED(10),
    VERSION(11),
    PING(12),
    PONG(13);

    companion object {
        fun fromValue(value: Int): Opcode? = entries.find { it.value == value }
    }
}

enum class PlaybackState(val value: Int) {
    IDLE(0),
    PLAYING(1),
    PAUSED(2);

    companion object {
        fun fromValue(value: Int): PlaybackState = entries.find { it.value == value } ?: IDLE
    }
}

// --- Sender messages ---

@Serializable
data class PlayMessage(
    val container: String,
    val url: String? = null,
    val content: String? = null,
    val time: Double? = null,
    val volume: Double? = null,
    val speed: Double? = null,
    val headers: Map<String, String>? = null
)

@Serializable
data class SeekMessage(
    val time: Double
)

@Serializable
data class SetVolumeMessage(
    val volume: Double
)

@Serializable
data class SetSpeedMessage(
    val speed: Double
)

@Serializable
data class VersionMessage(
    val version: Int
)

// --- Receiver messages ---

@Serializable
data class PlaybackUpdateMessage(
    val generationTime: Long? = null,
    val state: Int,
    val time: Double? = null,
    val duration: Double? = null,
    val speed: Double? = null
)

@Serializable
data class VolumeUpdateMessage(
    val generationTime: Long? = null,
    val volume: Double
)

@Serializable
data class PlaybackErrorMessage(
    val message: String
)

// --- Packet I/O ---

fun OutputStream.sendPacket(opcode: Opcode, body: String? = null) {
    val bodyBytes = body?.toByteArray(Charsets.UTF_8)
    val size = 1 + (bodyBytes?.size ?: 0) // opcode byte + body
    val header = ByteBuffer.allocate(5)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(size)
        .put(opcode.value.toByte())
        .array()
    write(header)
    bodyBytes?.let { write(it) }
    flush()
}

data class ReceivedPacket(val opcode: Opcode, val body: String?)

fun InputStream.readPacket(): ReceivedPacket? {
    val headerBuf = ByteArray(5)
    var read = 0
    while (read < 5) {
        val n = read(headerBuf, read, 5 - read)
        if (n == -1) return null
        read += n
    }
    val size = ByteBuffer.wrap(headerBuf, 0, 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .int
    val opcodeVal = headerBuf[4].toInt() and 0xFF
    val opcode = Opcode.fromValue(opcodeVal) ?: return null

    val bodySize = size - 1
    val body = if (bodySize > 0) {
        val bodyBuf = ByteArray(bodySize.coerceAtMost(FCastProtocol.MAX_PACKET_SIZE))
        var bodyRead = 0
        while (bodyRead < bodySize) {
            val n = read(bodyBuf, bodyRead, bodySize - bodyRead)
            if (n == -1) break
            bodyRead += n
        }
        String(bodyBuf, 0, bodyRead, Charsets.UTF_8)
    } else null

    return ReceivedPacket(opcode, body)
}
