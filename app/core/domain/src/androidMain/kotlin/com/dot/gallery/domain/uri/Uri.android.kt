package com.dot.gallery.domain.uri


actual typealias Uri = android.net.Uri

actual fun parseUri(uri: String): Uri = Uri.parse(uri)

actual fun parseUriOrNull(uri: String): Uri? =
    runCatching { Uri.parse(uri) }.getOrNull()?.takeIf { it.scheme != null }
