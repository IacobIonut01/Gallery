package com.dot.gallery.app.presentation.storycards

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.dot.gallery.domain.model.StoryCard
import com.dot.gallery.domain.model.StoryCardType
import com.dot.gallery.app.presentation.storycards.components.StoryCardsRow
import com.dot.gallery.app.presentation.storycards.components.storyCardTag
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.story_type_albums
import com.dot.gallery.core.util.getString



@MediumTest
@RunWith(AndroidJUnit4::class)
class StoryCardsRowTest {

    @get:Rule
    val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun card_exposesLocalizedDescriptionAndDeliversClick() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val card = StoryCard(
            id = 42L,
            type = StoryCardType.ALBUMS,
            title = "Camera",
            subtitle = "3 items",
        )
        var clickedCardId: Long? = null

        rule.setContent {
            MaterialTheme {
                StoryCardsRow(cards = listOf(card), onCardClick = { clickedCardId = it.id })
            }
        }

        rule.onNodeWithTag(storyCardTag(card.id))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertContentDescriptionEquals(
                listOf(
                    context.getString(Res.string.story_type_albums),
                    card.title,
                    card.subtitle,
                ).joinToString(". ")
            )
            .performClick()

        rule.runOnIdle { assertEquals(card.id, clickedCardId) }
    }
}
