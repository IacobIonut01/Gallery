/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.ui

import androidx.compose.runtime.Stable
import com.dot.gallery.feature.help.domain.HelpCategory
import com.dot.gallery.feature.help.domain.HelpRepository
import com.dot.gallery.feature.help.domain.HelpSearchItem
import com.dot.gallery.feature.help.domain.ReleaseNotes

@Stable
data class HelpState(
    /** Latest changelog release — drives the "What's new" hero card. Null hides it. */
    val currentRelease: ReleaseNotes? = null,
    /** Resolved, ready-to-score search entries (built via `HelpSearchIndex.build`). */
    val searchIndex: List<HelpSearchItem> = emptyList(),
    val getStartedCategories: List<HelpCategory> = HelpRepository.getGetStartedCategories(),
    val makeMostCategories: List<HelpCategory> = HelpRepository.getMakeMostCategories(),
    val exploreMoreCategories: List<HelpCategory> = HelpRepository.getExploreMoreCategories(),
)
