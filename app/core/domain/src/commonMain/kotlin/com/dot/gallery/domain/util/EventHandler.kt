package com.dot.gallery.domain.util

import com.dot.gallery.domain.model.UIEvent
import kotlinx.coroutines.flow.Flow

interface EventHandler {

    val updaterFlow: Flow<UIEvent>
    var navigateAction: (String) -> Unit
    var toggleNavigationBarAction: (Boolean) -> Unit
    var navigateUpAction: () -> Unit
    var setFollowThemeAction: (Boolean) -> Unit

    fun pushEvent(event: UIEvent)

}