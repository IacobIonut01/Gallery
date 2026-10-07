/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.ignored.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.presentation.components.LocalMediaImageRenderer
import com.dot.gallery.core.presentation.components.MockedMediaImageRenderer
import com.dot.gallery.core.ui.components.PreviewHost
import com.dot.gallery.domain.model.UIEvent
import com.dot.gallery.domain.util.EventHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** No-op [EventHandler] for previews — sheet steps render [com.dot.gallery.core.presentation.components.NavigationBackButton]. */
private class PreviewEventHandler : EventHandler {
    override val updaterFlow: Flow<UIEvent> = emptyFlow()
    override var navigateAction: (String) -> Unit = {}
    override var toggleNavigationBarAction: (Boolean) -> Unit = {}
    override var navigateUpAction: () -> Unit = {}
    override var setFollowThemeAction: (Boolean) -> Unit = {}
    override fun pushEvent(event: UIEvent) = Unit
}

/**
 * Feature-local preview host: [PreviewHost] plus the composition locals the
 * ignored-albums UI reads at composition time (event handler for the back
 * button, mocked thumbnails for album art).
 */
@Composable
internal fun IgnoredPreviewHost(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalEventHandler provides PreviewEventHandler(),
        LocalMediaImageRenderer provides MockedMediaImageRenderer,
    ) {
        PreviewHost(darkTheme = darkTheme, content = content)
    }
}
