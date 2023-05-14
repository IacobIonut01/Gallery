package com.dot.gallery.feature_node.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChanneledViewModel @Inject constructor() : ViewModel() {

    sealed class Event {
        data class NavigationRouteEvent(val route: String): Event()
        object NavigationUpEvent: Event()
    }

    private val eventChannel = Channel<Event>()

    fun initWithNav(navController: NavController) =
        eventChannel.receiveAsFlow().map {
            when (it) {
                is Event.NavigationRouteEvent ->
                    navController.navigate(it.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                is Event.NavigationUpEvent ->
                    navController.navigateUp()
            }
        }

    fun navigate(route: String) {
        viewModelScope.launch {
            eventChannel.send(Event.NavigationRouteEvent(route))
        }
    }

    fun navigateUp() {
        viewModelScope.launch {
            eventChannel.send(Event.NavigationUpEvent)
        }
    }

}