package com.dot.gallery

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import com.dot.gallery.core.presentation.components.util.OnLifecycleEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VaultLifecycleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun processLifecycleIsInitializedBeforeProtectedScreensObserveIt() {
        assertTrue(
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
        )
    }

    @Test
    fun explicitLifecycleOwnerIgnoresHostActivityLifecycle() {
        val lifecycleOwner = TestLifecycleOwner()
        val events = mutableListOf<Lifecycle.Event>()

        composeRule.setContent {
            OnLifecycleEvent(lifecycleOwner = lifecycleOwner) { _, event ->
                events += event
            }
        }

        composeRule.runOnIdle {
            assertTrue(events.isEmpty())
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        composeRule.runOnIdle {
            assertEquals(
                listOf(Lifecycle.Event.ON_CREATE, Lifecycle.Event.ON_START),
                events
            )
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry
    }
}
