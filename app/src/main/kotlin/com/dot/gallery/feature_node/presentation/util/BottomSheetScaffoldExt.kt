package com.dot.gallery.feature_node.presentation.util

/*
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.BottomSheetScaffoldState

@OptIn(ExperimentalMaterialApi::class)
val BottomSheetScaffoldState.currentFraction: Float
    get() {
        val origProgress = bottomSheetState.progress
        val fraction = if (bottomSheetState.isCollapsed) {
            if (origProgress != 1f) origProgress else 0f
        } else {
            if (origProgress == 1f) 1f else {
                val newProgress = 1f - origProgress
                if (newProgress == 0f) 1f else newProgress
            }
        }
        return fraction
    }
*/