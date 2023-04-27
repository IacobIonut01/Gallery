package com.dot.gallery.feature_node.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

open class ChanneledViewModel : ViewModel() {

    sealed class Event {
        data class NavigationRouteEvent(val route: String): Event()
        object NavigationUpEvent: Event()
    }

    private val eventChannel = Channel<Event>()

    fun initWithNav(navController: NavController) {
        eventChannel.receiveAsFlow().map {
            when (it) {
                is Event.NavigationRouteEvent ->
                    navController.navigate(it.route)
                is Event.NavigationUpEvent ->
                    navController.navigateUp()
            }
        }.launchIn(viewModelScope)
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