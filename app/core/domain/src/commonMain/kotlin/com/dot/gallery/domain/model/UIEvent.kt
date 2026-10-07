package com.dot.gallery.domain.model


sealed class UIEvent {
    data object UpdateDatabase : UIEvent()
    data class NavigationRouteEvent(val route: String) : UIEvent()
    data class ToggleNavigationBarEvent(val isVisible: Boolean) : UIEvent()
    data object NavigationUpEvent : UIEvent()
    data class SetFollowThemeEvent(val followTheme: Boolean) : UIEvent()
}