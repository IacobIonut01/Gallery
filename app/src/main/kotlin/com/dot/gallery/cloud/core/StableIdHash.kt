/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import java.security.MessageDigest

/**
 * Generates a stable, collision-resistant positive Long from a string.
 * Uses the first 8 bytes of a SHA-256 digest, masked to positive range.
 * Collision probability: ~1 in 4.6 × 10^18 (vs ~1 in 2 × 10^9 for hashCode).
 */
fun stableIdHash(input: String): Long {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    var result = 0L
    for (i in 0 until 8) {
        result = (result shl 8) or (digest[i].toLong() and 0xFFL)
    }
    return result and 0x7FFFFFFFFFFFFFFFL // ensure positive
}
