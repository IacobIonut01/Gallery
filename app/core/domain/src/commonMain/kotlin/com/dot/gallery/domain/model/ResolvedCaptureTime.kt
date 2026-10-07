/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model


data class ResolvedCaptureTime(
    val timestampMillis: Long,
    val origin: CaptureTimeOrigin
)
