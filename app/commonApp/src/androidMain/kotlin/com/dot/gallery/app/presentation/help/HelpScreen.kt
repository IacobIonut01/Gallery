/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.help

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.navigate
import com.dot.gallery.feature.help.data.ChangelogRepository
import com.dot.gallery.feature.help.domain.HelpSearchIndex
import com.dot.gallery.feature.help.domain.HelpSearchItem
import com.dot.gallery.feature.help.domain.SettingsSearchRegistry
import com.dot.gallery.feature.help.search.HelpFuzzyMatcher
import com.dot.gallery.feature.help.ui.HelpActions
import com.dot.gallery.feature.help.ui.HelpContent
import com.dot.gallery.feature.help.ui.HelpState
import com.dot.gallery.feature.help.ui.LocalReduceMotion
import com.dot.gallery.feature.help.ui.rememberReduceMotion
import com.dot.gallery.feature.help.toScreenRoute

/**
 * Thin app-side wrapper over [HelpContent]: loads the changelog from bundled
 * assets, builds the fuzzy search index on a coroutine, wires the fuzzywuzzy
 * engine and resolves [com.dot.gallery.feature.help.domain.HelpRoute] to
 * `Screen.*` routes.
 */
@Composable
fun HelpScreen() {
    val eventHandler = LocalEventHandler.current
    val context = LocalContext.current

    val currentRelease = remember { ChangelogRepository.getCurrent(context) }
    val registeredSettings by SettingsSearchRegistry.entries.collectAsStateWithLifecycle()
    val searchIndex by produceState<List<HelpSearchItem>>(emptyList(), registeredSettings) {
        value = HelpSearchIndex.build(
            releases = ChangelogRepository.getAll(context),
            settingsEntries = registeredSettings,
        )
    }

    CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion()) {
        HelpContent(
            state = HelpState(
                currentRelease = currentRelease,
                searchIndex = searchIndex,
            ),
            actions = HelpActions(
                onNavigate = { route -> eventHandler.navigate(route.toScreenRoute()) },
            ),
            searchEngine = HelpFuzzyMatcher,
        )
    }
}
