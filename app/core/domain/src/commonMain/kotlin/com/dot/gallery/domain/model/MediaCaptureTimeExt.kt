/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model


fun Media.timestampFor(source: TimelineDateSource): Long = when (source) {
    TimelineDateSource.CAPTURE_TIME -> definedTimestamp
    TimelineDateSource.MODIFIED_TIME -> timestamp
}

val Media.resolvedCaptureTimeOrigin: CaptureTimeOrigin
    get() = (this as? UriMedia)?.captureTimeOrigin
        ?: if (takenTimestamp != null) CaptureTimeOrigin.MEDIA_STORE
        else CaptureTimeOrigin.MODIFIED_FALLBACK
