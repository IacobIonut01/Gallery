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
import com.dot.gallery.feature.help.ui.TutorialCategoryContent
import com.dot.gallery.feature.help.ui.rememberReduceMotion
import com.dot.gallery.feature.help.toScreenRoute

/** Thin app-side wrapper over [TutorialCategoryContent]. */
@Composable
fun TutorialCategoryScreen(categoryName: String) {
    val eventHandler = LocalEventHandler.current
    CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion()) {
        TutorialCategoryContent(
            categoryName = categoryName,
            onNavigate = { route -> eventHandler.navigate(route.toScreenRoute()) },
        )
    }
}
