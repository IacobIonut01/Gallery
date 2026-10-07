package com.dot.gallery.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class AlbumMediaLoadMode(val skipBatching: Boolean) {
    Progressive(false),
    Complete(true)
}
