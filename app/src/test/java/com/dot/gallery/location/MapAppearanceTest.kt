package com.dot.gallery.location

import com.dot.gallery.feature_node.presentation.location.MapAppearance
import com.dot.gallery.feature_node.presentation.location.MapStyles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapAppearanceTest {
    @Test
    fun unknownStoredValueFallsBackToSystem() {
        assertEquals(MapAppearance.SYSTEM, MapAppearance.fromStored("unexpected"))
    }

    @Test
    fun systemFollowsEffectiveAppTheme() {
        assertFalse(MapAppearance.SYSTEM.resolvesDark(false))
        assertTrue(MapAppearance.SYSTEM.resolvesDark(true))
    }

    @Test
    fun forcedModesIgnoreAppTheme() {
        assertFalse(MapAppearance.LIGHT.resolvesDark(true))
        assertTrue(MapAppearance.DARK.resolvesDark(false))
    }

    @Test
    fun interactiveStyleUsesResolvedAppearance() {
        assertEquals(MapStyles.OPEN_FREE_MAP_LIGHT, MapStyles.interactiveStyle(MapAppearance.SYSTEM, false))
        assertEquals(MapStyles.OPEN_FREE_MAP_DARK, MapStyles.interactiveStyle(MapAppearance.SYSTEM, true))
        assertEquals(MapStyles.OPEN_FREE_MAP_LIGHT, MapStyles.interactiveStyle(MapAppearance.LIGHT, true))
        assertEquals(MapStyles.OPEN_FREE_MAP_DARK, MapStyles.interactiveStyle(MapAppearance.DARK, false))
    }
}
