package com.dot.gallery.feature_node.presentation.location

enum class MapAppearance(val storedValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun resolvesDark(effectiveAppIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> effectiveAppIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStored(value: String): MapAppearance =
            entries.firstOrNull { it.storedValue == value } ?: SYSTEM
    }
}

object MapStyles {
    const val OPEN_FREE_MAP_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
    const val OPEN_FREE_MAP_DARK = "https://tiles.openfreemap.org/styles/dark"

    fun interactiveStyle(appearance: MapAppearance, effectiveAppIsDark: Boolean): String =
        if (appearance.resolvesDark(effectiveAppIsDark)) OPEN_FREE_MAP_DARK else OPEN_FREE_MAP_LIGHT
}
