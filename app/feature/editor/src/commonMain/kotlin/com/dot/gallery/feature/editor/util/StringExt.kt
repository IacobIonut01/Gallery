package com.dot.gallery.feature.editor.util

inline fun String.sentenceCase(): String = lowercase().replaceFirstChar { it.uppercase() }
