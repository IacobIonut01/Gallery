/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.help

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.navigate
import com.dot.gallery.feature.help.data.ChangelogRepository
import com.dot.gallery.feature.help.ui.LocalReduceMotion
import com.dot.gallery.feature.help.ui.WhatsNewContent
import com.dot.gallery.feature.help.ui.rememberReduceMotion
import com.dot.gallery.feature.help.toScreenRoute

/**
 * Thin app-side wrapper over [WhatsNewContent]: loads releases from the
 * bundled changelog assets and resolves tip deep-links to `Screen.*` routes.
 */
@Composable
fun WhatsNewScreen() {
    val eventHandler = LocalEventHandler.current
    val context = LocalContext.current
    val allReleases = remember { ChangelogRepository.getAll(context) }

    CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion()) {
        WhatsNewContent(
            releases = allReleases,
            onNavigate = { route -> eventHandler.navigate(route.toScreenRoute()) },
        )
    }
}
