/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
/**
 * Represents a single Story Card to be displayed in the timeline carousel.
 * Each card has a type, title, subtitle, thumbnail, and the backing media list
 * that will be shown in the story viewer.
 */
@Stable
data class StoryCard(
    val id: Long,
    val type: StoryCardType,
    val title: String,
    val subtitle: String? = null,
    val thumbnailMedia: UriMedia? = null,
    val mediaList: List<UriMedia> = emptyList(),
    val albumId: Long? = null,
    val categoryId: Long? = null,
    val locationCity: String? = null,
    val locationCountry: String? = null,
    val year: Int? = null
)
