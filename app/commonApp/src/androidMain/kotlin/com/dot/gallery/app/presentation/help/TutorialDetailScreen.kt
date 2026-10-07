/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.help

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.navigate
import com.dot.gallery.feature.help.ui.LocalReduceMotion
import com.dot.gallery.feature.help.ui.TutorialDetailContent
import com.dot.gallery.app.presentation.help.previews.HelpPreview
import com.dot.gallery.feature.help.ui.rememberReduceMotion
import com.dot.gallery.feature.help.toScreenRoute

/**
 * Thin app-side wrapper over [TutorialDetailContent]: renders the step pages
 * and supplies the app-side [HelpPreview] dispatcher (live previews compose
 * real screens with mocked providers, which stay app-side).
 */
@Composable
fun TutorialDetailScreen(tipId: String) {
    val eventHandler = LocalEventHandler.current
    CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion()) {
        TutorialDetailContent(
            tipId = tipId,
            onNavigate = { route -> eventHandler.navigate(route.toScreenRoute()) },
            preview = { type -> HelpPreview(type) },
        )
    }
}
