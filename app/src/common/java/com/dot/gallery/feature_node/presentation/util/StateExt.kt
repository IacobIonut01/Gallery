package com.dot.gallery.feature_node.presentation.util

import androidx.compose.runtime.MutableState

fun <T> MutableState<T>.update(newState: T) {
    if (value != newState) {
        value = newState
    }
}