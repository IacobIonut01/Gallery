package com.dot.gallery.feature_node.presentation.edit.components.utils

import java.io.IOException

sealed interface SaveFileResult {

    data object Success : SaveFileResult
    class Failure(val exception: IOException) : SaveFileResult

}