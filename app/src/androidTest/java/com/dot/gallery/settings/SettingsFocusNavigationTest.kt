package com.dot.gallery.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.presentation.settings.components.SettingsItem
import com.dot.gallery.feature_node.presentation.settings.components.settingsFocusGroup
import com.dot.gallery.ui.theme.GalleryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsFocusNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dpadTraversesLongListAndActivatesLastItem() {
        var activationCount = 0
        composeRule.setContent {
            GalleryTheme {
                FocusedSettingsList(
                    items = List(12) { index ->
                        SettingsEntity.Preference(
                            title = "Setting $index",
                            onClick = { if (index == 11) activationCount++ },
                            screenPosition = positionFor(index, 12),
                        )
                    }
                )
            }
        }

        composeRule.onNodeWithTag("setting-0").assertIsFocused()
        repeat(11) {
            composeRule.onNodeWithTag("setting-$it").performKeyInput {
                pressKey(Key.DirectionDown)
            }
        }
        composeRule.onNodeWithTag("setting-11").assertIsFocused().performKeyInput {
            pressKey(Key.Enter)
        }
        assertEquals(1, activationCount)
    }

    @Test
    fun simpleSwitchIsOneFocusStopAndCenterTogglesOnce() {
        var toggleCount = 0
        composeRule.setContent {
            GalleryTheme {
                FocusedSettingsList(
                    items = listOf(
                        SettingsEntity.SwitchPreference(
                            title = "Switch",
                            isChecked = false,
                            onCheck = { toggleCount++ },
                            screenPosition = Position.Top,
                        ),
                        SettingsEntity.Preference(
                            title = "Next",
                            onClick = {},
                            screenPosition = Position.Bottom,
                        ),
                    )
                )
            }
        }

        composeRule.onNodeWithTag("setting-0").assertIsFocused().performKeyInput {
            pressKey(Key.Enter)
        }
        assertEquals(1, toggleCount)
        composeRule.onNodeWithTag("setting-0").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.onNodeWithTag("setting-1").assertIsFocused()
    }

    @Test
    fun headersAndDisabledRowsAreSkipped() {
        composeRule.setContent {
            GalleryTheme {
                FocusedSettingsList(
                    items = listOf(
                        SettingsEntity.Preference(
                            title = "First",
                            onClick = {},
                            screenPosition = Position.Alone,
                        ),
                        SettingsEntity.Header("Header"),
                        SettingsEntity.Preference(
                            title = "Disabled",
                            enabled = false,
                            onClick = {},
                            screenPosition = Position.Top,
                        ),
                        SettingsEntity.Preference(
                            title = "Last",
                            onClick = {},
                            screenPosition = Position.Bottom,
                        ),
                    )
                )
            }
        }

        composeRule.onNodeWithTag("setting-0").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.onNodeWithTag("setting-1").assertIsNotFocused()
        composeRule.onNodeWithTag("setting-2").assertIsNotFocused()
        composeRule.onNodeWithTag("setting-3").assertIsFocused()
    }

    @Composable
    private fun FocusedSettingsList(items: List<SettingsEntity>) {
        val inputModeManager = LocalInputModeManager.current
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            inputModeManager.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }
        Box(modifier = Modifier.height(240.dp)) {
            LazyColumn(modifier = Modifier.settingsFocusGroup()) {
                itemsIndexed(items) { index, item ->
                    SettingsItem(
                        item = item,
                        modifier = Modifier
                            .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                            .testTag("setting-$index"),
                    )
                }
            }
        }
    }

    private fun positionFor(index: Int, size: Int): Position = when (index) {
        0 -> Position.Top
        size - 1 -> Position.Bottom
        else -> Position.Middle
    }
}
