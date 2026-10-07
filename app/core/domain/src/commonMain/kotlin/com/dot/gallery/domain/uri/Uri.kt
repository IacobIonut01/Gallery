package com.dot.gallery.domain.uri


/**
 * Multiplatform stand-in for `android.net.Uri`.
 *
 * On Android this typealiases to the platform class, so every existing call
 * site (ContentResolver, MediaStore, Glide) keeps working unchanged. On
 * wasmJs it is a thin wrapper over `com.eygraber.uri.Uri`, a pure-Kotlin port
 * of the Android implementation.
 *
 * Java's getter methods do not map to properties for expect/actual matching,
 * so the members are declared as functions; the property-syntax extensions
 * below keep `uri.scheme` working in common code.
 */
expect abstract class Uri {
    abstract fun getScheme(): String?
    abstract fun getAuthority(): String?
    abstract fun getHost(): String?
    abstract fun getPath(): String?
    abstract fun getEncodedPath(): String?
    abstract fun getQuery(): String?
    abstract fun getFragment(): String?
    abstract fun getPathSegments(): List<String>
    abstract fun getLastPathSegment(): String?
    open fun isAbsolute(): Boolean
    open fun isOpaque(): Boolean
    abstract override fun toString(): String
}

val Uri.scheme: String? get() = getScheme()
val Uri.authority: String? get() = getAuthority()
val Uri.host: String? get() = getHost()
val Uri.path: String? get() = getPath()
val Uri.encodedPath: String? get() = getEncodedPath()
val Uri.query: String? get() = getQuery()
val Uri.fragment: String? get() = getFragment()
val Uri.pathSegments: List<String> get() = getPathSegments()
val Uri.lastPathSegment: String? get() = getLastPathSegment()

/** Parses [uri] into a [Uri] — `Uri.parse` on Android, eygraber on wasmJs. */
expect fun parseUri(uri: String): Uri

/** Parses [uri] into a [Uri] or null when the string is malformed. */
expect fun parseUriOrNull(uri: String): Uri?

/** Empty [Uri] — `Uri.EMPTY` on Android. */
val emptyUri: Uri get() = parseUri("")

/**
 * Decodes percent-escaped triplets in [encoded] — `android.net.Uri.decode`
 * semantics: `%XX` sequences decode to UTF-8 text, `+` stays a literal plus
 * (unlike `URLDecoder`), and malformed escapes pass through unchanged.
 */
fun decodeUri(encoded: String): String {
    if ('%' !in encoded) return encoded
    val out = StringBuilder(encoded.length)
    val bytes = ByteArray(encoded.length / 3 + 1)
    var pending = 0
    fun flush() {
        if (pending > 0) {
            out.append(bytes.copyOf(pending).decodeToString())
            pending = 0
        }
    }
    var i = 0
    while (i < encoded.length) {
        val c = encoded[i]
        if (c == '%' && i + 2 < encoded.length) {
            val hi = encoded[i + 1].hexDigit()
            val lo = encoded[i + 2].hexDigit()
            if (hi >= 0 && lo >= 0) {
                bytes[pending++] = ((hi shl 4) or lo).toByte()
                i += 3
                continue
            }
        }
        flush()
        out.append(c)
        i++
    }
    flush()
    return out.toString()
}

private fun Char.hexDigit() = when (this) {
    in '0'..'9' -> this - '0'
    in 'a'..'f' -> this - 'a' + 10
    in 'A'..'F' -> this - 'A' + 10
    else -> -1
}
