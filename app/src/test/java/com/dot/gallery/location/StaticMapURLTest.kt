package com.dot.gallery.location

import com.dot.gallery.feature_node.presentation.location.MapAppearance
import com.dot.gallery.feature_node.presentation.util.StaticMapURL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticMapURLTest {
    @Test
    fun appearanceSelectsMatchingTileProvider() {
        val light = StaticMapURL(46.77, 23.59, MapAppearance.LIGHT, effectiveAppIsDark = true)
        val dark = StaticMapURL(46.77, 23.59, MapAppearance.DARK, effectiveAppIsDark = false)
        assertTrue(light.contains("rastertiles/voyager"))
        assertTrue(dark.contains("rastertiles/dark_all"))
    }

    @Test
    fun systemUsesEffectiveAppTheme() {
        val light = StaticMapURL(0.0, 0.0, MapAppearance.SYSTEM, effectiveAppIsDark = false)
        val dark = StaticMapURL(0.0, 0.0, MapAppearance.SYSTEM, effectiveAppIsDark = true)
        assertTrue(light.contains("voyager"))
        assertTrue(dark.contains("dark_all"))
    }

    @Test
    fun coordinatesAreClampedAndWrapped() {
        val url = StaticMapURL(1000.0, 540.0, MapAppearance.LIGHT, zoom = 8)
        val parts = url.substringAfter("voyager/").substringBefore("@2x.png").split('/')
        assertEquals("8", parts[0])
        assertTrue(parts[1].toInt() in 0..255)
        assertTrue(parts[2].toInt() in 0..255)
    }
}
