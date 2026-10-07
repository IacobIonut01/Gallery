package com.dot.gallery.app.presentation.storycards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.dot.gallery.core.presentation.components.AppBarContainer
import com.dot.gallery.core.ui.settings.LocalSettingsStore
import com.dot.gallery.domain.settings.MapSettingsStore
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.core.presentation.components.AppBarOverlayContentTag



@MediumTest
@RunWith(AndroidJUnit4::class)
class StoryCardsOverlayHostTest {

    @get:Rule
    val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun visibleOverlay_removesUnderlyingShellFromAccessibilityTraversal() {
        rule.setContent {
            val navController = rememberNavController()
            CompositionLocalProvider(LocalSettingsStore provides MapSettingsStore()) {
            MaterialTheme {
                AppBarContainer(
                    navController = navController,
                    bottomBarState = false,
                    paddingValues = PaddingValues(),
                    isScrolling = false,
                    overlayVisible = true,
                    overlayContent = { _, _, _ ->
                        Box(Modifier.fillMaxSize())
                    },
                ) { _ ->
                    Box(Modifier.fillMaxSize()) {
                        Text("Underlying content")
                    }
                }
            }
            }
        }

        rule.onNodeWithTag(AppBarOverlayContentTag).assertIsDisplayed()
        rule.onNodeWithText("Underlying content").assertDoesNotExist()
    }
}
