package com.dot.gallery.feature_node.presentation.search

import com.dot.gallery.feature_node.data.data_source.SmartScanStatus

data class SearchIndexerState(
    val isIndexing: Boolean = false,
    val status: SmartScanStatus? = null,
    val progress: Float = 0f,
    val processed: Int = 0,
    val total: Int = 0,
    val stageNumber: Int = 0,
    val stageCount: Int = 0,
    val estimatedRemainingMillis: Long? = null
)
