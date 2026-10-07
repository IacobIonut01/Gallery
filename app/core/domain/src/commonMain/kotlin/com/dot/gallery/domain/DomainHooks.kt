/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain


/**
 * Host-provided values the domain layer needs but cannot compute itself —
 * the app sets them once at startup (see `GalleryApp`). Defaults are inert:
 * empty [applicationId] makes [com.dot.gallery.domain.util.isEncrypted] false,
 * a null [errorLogger] makes [reportError] fall back to the platform log.
 */
object DomainHooks {

    var applicationId: String = ""

    var errorLogger: ((tag: String, message: String, throwable: Throwable?) -> Unit)? = null

    /**
     * HTTP(S) ranged-GET used by `RemoteMediaProvider.fetchRange` — the provider SPI lives in
     * this module while the okhttp client stays app-side. Returns null on any failure.
     */
    var httpRangeFetcher: (suspend (url: String, offset: Long, length: Long, headers: Map<String, String>) -> ByteArray?)? = null
}

fun reportError(tag: String, message: String, throwable: Throwable? = null) {
    DomainHooks.errorLogger?.invoke(tag, message, throwable) ?: platformLogError(
        tag,
        message,
        throwable
    )
}

expect fun platformLogError(tag: String, message: String, throwable: Throwable?)
