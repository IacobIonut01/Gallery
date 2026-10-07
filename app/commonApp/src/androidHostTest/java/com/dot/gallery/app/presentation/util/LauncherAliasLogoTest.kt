package com.dot.gallery.app.presentation.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test



class LauncherAliasLogoTest {

    @Test
    fun `gallery logo aliases are detected`() {
        assertTrue(launcherAliasHasGalleryLogo("Launcher_ReFra_GalleryLogo"))
        assertTrue(launcherAliasHasGalleryLogo("Launcher_Gallery_GalleryLogo"))
    }

    @Test
    fun `refra logo aliases are not detected`() {
        assertFalse(launcherAliasHasGalleryLogo("Launcher_ReFra"))
        assertFalse(launcherAliasHasGalleryLogo("Launcher_Gallery"))
        assertFalse(launcherAliasHasGalleryLogo(""))
    }

    @Test
    fun `launcherAliasFor marks gallery logo aliases`() {
        assertTrue(launcherAliasHasGalleryLogo(launcherAliasFor("ReFra", "Gallery")))
        assertTrue(launcherAliasHasGalleryLogo(launcherAliasFor("Gallery", "Gallery")))
        assertFalse(launcherAliasHasGalleryLogo(launcherAliasFor("ReFra", "ReFra")))
        assertFalse(launcherAliasHasGalleryLogo(launcherAliasFor("Gallery", "ReFra")))
    }
}
