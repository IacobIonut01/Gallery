package com.dot.gallery.core

import androidx.compose.runtime.compositionLocalOf
import com.dot.gallery.feature_node.domain.model.UIEvent
import com.dot.gallery.feature_node.domain.util.EventHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

val LocalEventHandler = compositionLocalOf<EventHandler> {
    error("No EventHandler provided!!! This is likely due to a missing Hilt injection in the Composable hierarchy.")
}

class DefaultEventHandler : EventHandler {

    // Use an unlimited buffer so trySend never drops events. A zero-capacity
    // (rendezvous) channel only delivers when a receiver is suspended at that exact
    // moment; under main-thread load (e.g. constant recomposition during video
    // playback) the collector is often busy and events like NavigationUpEvent were
    // silently dropped, causing the back gesture to be ignored on videos.
    private val updater = Channel<UIEvent>(capacity = Channel.UNLIMITED)
    override val updaterFlow = updater.receiveAsFlow()
    override var navigateAction: (String) -> Unit = {}
    override var toggleNavigationBarAction: (Boolean) -> Unit = {}
    override var navigateUpAction: () -> Unit = {}
    override var setFollowThemeAction: (Boolean) -> Unit = {}

    override fun pushEvent(event: UIEvent) {
        updater.trySend(event)
    }

}

fun EventHandler.navigateUp() = pushEvent(UIEvent.NavigationUpEvent)
fun EventHandler.navigate(route: String) = pushEvent(UIEvent.NavigationRouteEvent(route))
fun EventHandler.toggleNavigationBar(show: Boolean) = pushEvent(UIEvent.ToggleNavigationBarEvent(show))
fun EventHandler.setFollowTheme(followTheme: Boolean) = pushEvent(UIEvent.SetFollowThemeEvent(followTheme))