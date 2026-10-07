/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.dot.gallery.core.presentation.util.StaticMapTilePlacement


@MediumTest
@RunWith(AndroidJUnit4::class)
class StaticMapPreviewTest {

    @get:Rule
    val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun wideViewportTilesKeepSquareSizeAndAbsoluteOffsets() {
        rule.setContent {
            TileGridFixture(
                viewportSize = DpSize(320.dp, 160.dp),
                placements = tilePlacements(320.dp, LocalDensity.current),
            )
        }

        assertTilesPlacedSquareAtOffsets(tileSize = 320.dp)
    }

    @Test
    fun rtlWideViewportTilesKeepAbsoluteOffsets() {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TileGridFixture(
                    viewportSize = DpSize(320.dp, 160.dp),
                    placements = tilePlacements(320.dp, LocalDensity.current),
                )
            }
        }

        assertTilesPlacedSquareAtOffsets(tileSize = 320.dp)
    }

    @Test
    fun squareViewportTilesKeepSquareSizeAndAbsoluteOffsets() {
        rule.setContent {
            TileGridFixture(
                viewportSize = DpSize(160.dp, 160.dp),
                placements = tilePlacements(160.dp, LocalDensity.current),
            )
        }

        assertTilesPlacedSquareAtOffsets(tileSize = 160.dp)
    }

    @Test
    fun tilesStaySquareWhenViewportResizesBetweenAspects() {
        var viewportSize by mutableStateOf(DpSize(320.dp, 160.dp))
        rule.setContent {
            val tileSize = maxOf(viewportSize.width, viewportSize.height)
            TileGridFixture(
                viewportSize = viewportSize,
                placements = tilePlacements(tileSize, LocalDensity.current),
            )
        }

        assertTilesPlacedSquareAtOffsets(tileSize = 320.dp)

        rule.runOnIdle { viewportSize = DpSize(160.dp, 320.dp) }
        rule.waitForIdle()

        assertTilesPlacedSquareAtOffsets(tileSize = 320.dp)
    }

    private fun tilePlacements(tileSize: Dp, density: Density): List<StaticMapTilePlacement> {
        fun px(dp: Dp) = with(density) { dp.toPx() }
        return listOf(
            StaticMapTilePlacement(
                tileX = 0,
                tileY = 0,
                leftPx = px(tileSize * -0.75f),
                topPx = px(tileSize * -0.65625f),
                sizePx = px(tileSize),
            ),
            StaticMapTilePlacement(
                tileX = 1,
                tileY = 0,
                leftPx = px(tileSize * 0.25f),
                topPx = px(tileSize * -0.65625f),
                sizePx = px(tileSize),
            ),
            StaticMapTilePlacement(
                tileX = 0,
                tileY = 1,
                leftPx = px(tileSize * -0.75f),
                topPx = px(tileSize * 0.34375f),
                sizePx = px(tileSize),
            ),
            StaticMapTilePlacement(
                tileX = 1,
                tileY = 1,
                leftPx = px(tileSize * 0.25f),
                topPx = px(tileSize * 0.34375f),
                sizePx = px(tileSize),
            ),
        )
    }

    private fun assertTilesPlacedSquareAtOffsets(tileSize: Dp) {
        val viewportBounds = rule.onNodeWithTag(ViewportTag).getUnclippedBoundsInRoot()
        assertTile(0, 0, tileSize * -0.75f, tileSize * -0.65625f, tileSize, viewportBounds)
        assertTile(1, 0, tileSize * 0.25f, tileSize * -0.65625f, tileSize, viewportBounds)
        assertTile(0, 1, tileSize * -0.75f, tileSize * 0.34375f, tileSize, viewportBounds)
        assertTile(1, 1, tileSize * 0.25f, tileSize * 0.34375f, tileSize, viewportBounds)
    }

    private fun assertTile(
        tileX: Int,
        tileY: Int,
        expectedLeft: Dp,
        expectedTop: Dp,
        expectedSize: Dp,
        viewportBounds: DpRect,
    ) {
        val bounds = rule.onNodeWithTag(tileTag(tileX, tileY)).getUnclippedBoundsInRoot()
        (bounds.right - bounds.left).assertIsEqualTo(
            expectedSize,
            "width of tile ($tileX,$tileY)",
        )
        (bounds.bottom - bounds.top).assertIsEqualTo(
            expectedSize,
            "height of tile ($tileX,$tileY)",
        )
        (bounds.left - viewportBounds.left).assertIsEqualTo(
            expectedLeft,
            "left offset of tile ($tileX,$tileY) relative to viewport",
        )
        (bounds.top - viewportBounds.top).assertIsEqualTo(
            expectedTop,
            "top offset of tile ($tileX,$tileY) relative to viewport",
        )
    }

    @Composable
    private fun TileGridFixture(
        viewportSize: DpSize,
        placements: List<StaticMapTilePlacement>,
    ) {
        Box(
            Modifier
                .size(viewportSize)
                .clipToBounds()
                .testTag(ViewportTag),
        ) {
            StaticMapTileGrid(placements, Modifier.fillMaxSize()) { placement ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(tileColor(placement))
                        .testTag(tileTag(placement.tileX, placement.tileY)),
                )
            }
        }
    }

    private fun tileColor(placement: StaticMapTilePlacement): Color =
        TileColors[(placement.tileX + placement.tileY * 2).mod(TileColors.size)]

    private fun tileTag(tileX: Int, tileY: Int) = "tile-$tileX-$tileY"

    private companion object {
        const val ViewportTag = "viewport"
        val TileColors = listOf(
            Color(0xFFD32F2F),
            Color(0xFF388E3C),
            Color(0xFF1976D2),
            Color(0xFFFBC02D),
        )
    }
}
