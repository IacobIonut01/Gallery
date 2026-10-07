/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data

object FloatVectorCodec {
    fun encode(values: FloatArray): ByteArray {
        val out = ByteArray(values.size * Float.SIZE_BYTES)
        for (i in values.indices) {
            val bits = values[i].toRawBits()
            val o = i * Float.SIZE_BYTES
            out[o] = (bits ushr 24).toByte()
            out[o + 1] = (bits ushr 16).toByte()
            out[o + 2] = (bits ushr 8).toByte()
            out[o + 3] = bits.toByte()
        }
        return out
    }

    fun decode(bytes: ByteArray): FloatArray {
        require(bytes.size % Float.SIZE_BYTES == 0) { "Invalid float vector byte count" }
        return FloatArray(bytes.size / Float.SIZE_BYTES) { i ->
            val o = i * Float.SIZE_BYTES
            Float.fromBits(
                (bytes[o].toInt() and 0xFF shl 24) or
                    (bytes[o + 1].toInt() and 0xFF shl 16) or
                    (bytes[o + 2].toInt() and 0xFF shl 8) or
                    (bytes[o + 3].toInt() and 0xFF)
            )
        }
    }
}
