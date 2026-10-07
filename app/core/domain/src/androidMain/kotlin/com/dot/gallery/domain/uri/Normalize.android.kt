package com.dot.gallery.domain.uri

import java.text.Normalizer

actual fun String.normalizeNfkc(): String = Normalizer.normalize(this, Normalizer.Form.NFKC)
