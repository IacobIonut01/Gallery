/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.domain

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource

@Immutable
data class HelpTip(
    val id: String,
    val title: StringResource,
    val subtitle: StringResource,
    val icon: HelpIcon,
    val category: HelpCategory,
    val pages: List<TutorialPage>,
    val deepLink: HelpRoute? = null,
    val sinceVersion: String? = null,
    /** Extra synonym/keyword string resources that feed the fuzzy search index. */
    val keywords: List<StringResource> = emptyList(),
    /** Live-navigation quick actions rendered at the bottom of the tip detail. */
    val quickActions: List<QuickAction> = emptyList()
)
