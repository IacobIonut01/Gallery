/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.domain.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Configuration for which Story Card types are enabled and their display order.
 */
@Serializable
data class StoryCardsConfig(
    val enabled: Boolean = true,
    val cardOrder: List<StoryCardType> = StoryCardType.entries.toList(),
    val disabledTypes: Set<StoryCardType> = emptySet()
) {
    /** cardOrder with any newly-added types appended (handles config persisted before the type existed). */
    val normalizedOrder: List<StoryCardType>
        get() = (cardOrder + StoryCardType.entries).distinct()

    val activeTypes: List<StoryCardType>
        get() = if (enabled) normalizedOrder.filter { it !in disabledTypes } else emptyList()
}
