package com.dot.gallery.core

import androidx.compose.runtime.compositionLocalOf

val LocalMediaDistributor = compositionLocalOf<MediaDistributor> {
    error("No MediaDistributor provided!!! This is likely due to a missing inject binding in the Composable hierarchy.")
}
