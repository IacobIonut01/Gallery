/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.timeline

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dot.gallery.feature.timeline.TimelineSortOption
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.feature.timeline.TIMELINE_CAPTURE_SORT_TAG


@MediumTest
@RunWith(AndroidJUnit4::class)
class TimelineSortOptionAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule(StandardTestDispatcher())

    @Test
    fun captureTimeOption_exposesRadioSelectionAndTouchTarget() {
        var selected by mutableStateOf(false)
        composeRule.setContent {
            MaterialTheme {
                TimelineSortOption(
                    title = "Capture time",
                    summary = "Embedded metadata",
                    testTag = TIMELINE_CAPTURE_SORT_TAG,
                    selected = selected,
                    onClick = { selected = true }
                )
            }
        }

        composeRule.onNodeWithTag(TIMELINE_CAPTURE_SORT_TAG)
            .assertHasClickAction()
            .assertIsNotSelected()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
            .assertIsSelected()
    }
}
