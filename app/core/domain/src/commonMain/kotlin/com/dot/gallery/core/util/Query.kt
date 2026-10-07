package com.dot.gallery.core.util

import com.dot.gallery.domain.uri.query

class Query(val root: Node) {
    fun build() = root.build()

    companion object {
        const val ARG = "?"
    }
}