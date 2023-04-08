package com.dot.gallery.feature_node.domain.util

sealed class OrderType {
    object Ascending : OrderType()
    object Descending : OrderType()
}
