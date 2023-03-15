package com.dot.gallery.feature_node.domain.util

sealed class MediaOrder(val orderType: OrderType) {
    class Label(orderType: OrderType): MediaOrder(orderType)
    class Date(orderType: OrderType): MediaOrder(orderType)

    fun copy(orderType: OrderType): MediaOrder {
        return when(this) {
            is Date -> Date(orderType)
            is Label -> Label(orderType)
        }
    }
}