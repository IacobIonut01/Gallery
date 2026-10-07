/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.docs

import com.dot.gallery.domain.model.UIEvent
import com.dot.gallery.domain.util.EventHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * No-op [EventHandler] for docs scenes — absorbs navigation/back events so
 * shared composables (e.g. NavigationBackButton) work without an app host.
 */
class DocsEventHandler : EventHandler {
    override val updaterFlow: Flow<UIEvent> = emptyFlow()
    override var navigateAction: (String) -> Unit = {}
    override var toggleNavigationBarAction: (Boolean) -> Unit = {}
    override var navigateUpAction: () -> Unit = {}
    override var setFollowThemeAction: (Boolean) -> Unit = {}
    override fun pushEvent(event: UIEvent) = Unit
}
