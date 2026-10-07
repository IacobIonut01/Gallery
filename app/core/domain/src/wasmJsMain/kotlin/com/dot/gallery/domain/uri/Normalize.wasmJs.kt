package com.dot.gallery.domain.uri


@JsFun("(s) => s.normalize('NFKC')")
private external fun jsNormalizeNfkc(s: String): String

actual fun String.normalizeNfkc(): String = jsNormalizeNfkc(this)
