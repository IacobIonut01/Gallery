/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Represents the type of a Story Card displayed above the timeline.
 */
@Serializable
enum class StoryCardType {
    MEMORIES,
    ALBUMS,
    CATEGORIES,
    LOCATIONS,
    FAVORITES,
    CLOUD_MEMORIES
}
