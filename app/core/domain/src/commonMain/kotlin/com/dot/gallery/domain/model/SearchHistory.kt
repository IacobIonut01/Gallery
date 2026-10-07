package com.dot.gallery.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchHistory(
    val timestamp: Long,
    val query: String,
    val mediaId: Long? = null,
    val mediaLabel: String? = null,
    val mediaUri: String? = null,
)