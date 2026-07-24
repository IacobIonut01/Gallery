package com.dot.gallery.feature_node.presentation.settings.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class SettingsFocusState internal constructor() {
    var hasFocus by mutableStateOf(false)
        internal set
}

@Composable
fun rememberSettingsFocusState(): SettingsFocusState = remember { SettingsFocusState() }

@Composable
fun Modifier.settingsFocusTarget(
    state: SettingsFocusState = rememberSettingsFocusState(),
    focusRequester: FocusRequester? = null,
): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val relocationJob = remember { arrayOfNulls<Job>(1) }
    val requesterModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }
    return this
        .then(requesterModifier)
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged { focusState ->
            state.hasFocus = focusState.hasFocus
            if (focusState.hasFocus) {
                relocationJob[0]?.cancel()
                relocationJob[0] = scope.launch {
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }
}

fun Modifier.settingsFocusGroup(): Modifier = focusGroup()

@Composable
fun RequestInitialSettingsFocus(
    focusRequester: FocusRequester,
    enabled: Boolean = true,
    prepareFocus: suspend () -> Unit = {},
) {
    val inputModeManager = LocalInputModeManager.current
    val inputMode = inputModeManager.inputMode
    LaunchedEffect(focusRequester, enabled, inputMode) {
        if (enabled && inputMode == InputMode.Keyboard) {
            prepareFocus()
            focusRequester.requestFocus()
        }
    }
}
