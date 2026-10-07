/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.util

private val K = intArrayOf(
    0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b,
    0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
    -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3,
    0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
    -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc,
    0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039,
    -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
    0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
    -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d,
    -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
    0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8,
    -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e
)

/** Pure-Kotlin SHA-256 — java.security.MessageDigest is JVM-only. */
fun sha256(input: ByteArray): ByteArray {
    var h = intArrayOf(
        0x6a09e667, -0x4498517b, -0x3705585f, 0x510e527f,
        -0x6791b02a, 0x1f83d9ab, 0x5be0cd19, 0x3079dd46
    )

    val bitLen = input.size.toLong() * 8
    val padded = input + 0x80.toByte() +
        ByteArray((56 - (input.size + 1) % 64 + 64) % 64) +
        ByteArray(8) { (bitLen ushr (56 - it * 8)).toByte() }

    val w = IntArray(64)
    for (chunk in padded.indices step 64) {
        for (t in 0 until 16) {
            val o = chunk + t * 4
            w[t] = (padded[o].toInt() and 0xFF shl 24) or
                (padded[o + 1].toInt() and 0xFF shl 16) or
                (padded[o + 2].toInt() and 0xFF shl 8) or
                (padded[o + 3].toInt() and 0xFF)
        }
        for (t in 16 until 64) {
            val s0 = w[t - 15].rotateRight(7) xor w[t - 15].rotateRight(18) xor (w[t - 15] ushr 3)
            val s1 = w[t - 2].rotateRight(17) xor w[t - 2].rotateRight(19) xor (w[t - 2] ushr 10)
            w[t] = w[t - 16] + s0 + w[t - 7] + s1
        }

        var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
        var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]

        for (t in 0 until 64) {
            val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
            val ch = (e and f) xor (e.inv() and g)
            val t1 = hh + s1 + ch + K[t] + w[t]
            val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val t2 = s0 + maj
            hh = g; g = f; f = e; e = d + t1
            d = c; c = b; b = a; a = t1 + t2
        }

        h[0] += a; h[1] += b; h[2] += c; h[3] += d
        h[4] += e; h[5] += f; h[6] += g; h[7] += hh
    }

    return ByteArray(32) { i -> (h[i / 4] ushr (24 - (i % 4) * 8)).toByte() }
}
