/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@MediumTest
@RunWith(AndroidJUnit4::class)
class StartupContentEffectTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun readyContentReleasesTheGateAfterTheDraw() {
        val gate = StartupWorkGate()
        var label by mutableStateOf("content")
        rule.setContent {
            CompositionLocalProvider(LocalStartupWorkGate provides gate) {
                Text(label)
                StartupContentEffect(route = "test", ready = true)
            }
        }
        rule.onNodeWithText("content").assertIsDisplayed()
        rule.runOnUiThread { label = "updated" }
        rule.waitUntil("gate released after draw", 5_000) { gate.isReleased }
        assertTrue(gate.isReleased)
    }

    @Test
    fun unreadyContentNeverReleasesTheGate() {
        val gate = StartupWorkGate()
        rule.setContent {
            CompositionLocalProvider(LocalStartupWorkGate provides gate) {
                Text("content")
                StartupContentEffect(route = "test", ready = false)
            }
        }
        rule.onNodeWithText("content").assertIsDisplayed()
        rule.waitForIdle()
        assertFalse(gate.isReleased)
    }

    @Test
    fun releaseGateFalseLeavesTheGateHeld() {
        val gate = StartupWorkGate()
        rule.setContent {
            CompositionLocalProvider(LocalStartupWorkGate provides gate) {
                Text("content")
                StartupContentEffect(route = "test", ready = true, releaseGate = false)
            }
        }
        rule.onNodeWithText("content").assertIsDisplayed()
        rule.waitForIdle()
        assertFalse(gate.isReleased)
    }

    @Test
    fun missingGateIsSkippedWithoutEffect() {
        rule.setContent {
            StartupContentEffect(route = "test", ready = true)
            Text("content")
        }
        rule.onNodeWithText("content").assertIsDisplayed()
    }
}
