package com.dot.gallery.domain.uri

import com.eygraber.uri.Uri as KmpUri

actual abstract class Uri private constructor() {

    internal abstract val delegate: KmpUri

    actual abstract fun getScheme(): String?
    actual abstract fun getAuthority(): String?
    actual abstract fun getHost(): String?
    actual abstract fun getPath(): String?
    actual abstract fun getEncodedPath(): String?
    actual abstract fun getQuery(): String?
    actual abstract fun getFragment(): String?
    actual abstract fun getPathSegments(): List<String>
    actual abstract fun getLastPathSegment(): String?
    actual open fun isAbsolute(): Boolean = delegate.isAbsolute
    actual open fun isOpaque(): Boolean = delegate.isOpaque
    actual abstract override fun toString(): String

    internal class Impl(override val delegate: KmpUri) : Uri() {
        override fun getScheme(): String? = delegate.scheme
        override fun getAuthority(): String? = delegate.authority
        override fun getHost(): String? = delegate.host
        override fun getPath(): String? = delegate.path
        override fun getEncodedPath(): String? = delegate.encodedPath
        override fun getQuery(): String? = delegate.query
        override fun getFragment(): String? = delegate.fragment
        override fun getPathSegments(): List<String> = delegate.pathSegments
        override fun getLastPathSegment(): String? = delegate.lastPathSegment
        override fun toString(): String = delegate.toString()

        override fun equals(other: Any?): Boolean =
            other is Impl && delegate == other.delegate

        override fun hashCode(): Int = delegate.hashCode()
    }
}

actual fun parseUri(uri: String): Uri = Uri.Impl(KmpUri.parse(uri))

actual fun parseUriOrNull(uri: String): Uri? = KmpUri.parseOrNull(uri)?.let(Uri::Impl)
