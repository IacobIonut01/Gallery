/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable

@Serializable
data class DetectedFace(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val embedding: FloatArray? = null,
    val confidence: Float = 0f,
    val personId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetectedFace) return false
        return left == other.left && top == other.top &&
                right == other.right && bottom == other.bottom &&
                personId == other.personId
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + (personId?.hashCode() ?: 0)
        return result
    }
}
