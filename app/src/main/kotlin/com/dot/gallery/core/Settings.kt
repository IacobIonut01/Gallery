/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dot.gallery.core.Settings.PREFERENCE_NAME
import com.dot.gallery.core.util.rememberPreference
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.theme.Dimens
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

/**
 * TODO: Create 'Preference' annotation to generate the composable remember functions automatically
 */
object Settings {

    const val PREFERENCE_NAME = "settings"

    object Album {
        private val LAST_SORT = intPreferencesKey("album_last_sort")
        @Composable
        fun rememberLastSort() =
            rememberPreference(key = LAST_SORT, defaultValue = 0)
        fun getLastSort(context: Context) =
            context.dataStore.data.map { it[LAST_SORT] ?: 0 }

        private val ALBUM_SIZE = floatPreferencesKey("album_size")

        @Composable
        fun rememberAlbumSize() =
            rememberPreference(key = ALBUM_SIZE, defaultValue = Dimens.Album.size.value)
    }

    object Glide {
        private val DISK_CACHE_SIZE = longPreferencesKey("disk_cache_size")
        @Composable
        fun rememberDiskCacheSize() =
            rememberPreference(key = DISK_CACHE_SIZE, defaultValue = 150)
        fun getDiskCacheSize(context: Context) =
            context.dataStore.data.map { it[DISK_CACHE_SIZE] ?: 150 }

        private val CACHED_SCREEN_COUNT = floatPreferencesKey("cached_screen_count")
        @Composable
        fun rememberCachedScreenCount() =
            rememberPreference(key = CACHED_SCREEN_COUNT, defaultValue = 80f)
        fun getCachedScreenCount(context: Context) =
            context.dataStore.data.map { it[CACHED_SCREEN_COUNT] ?: 80f }

        private val MAX_IMAGE_SIZE = intPreferencesKey("max_image_size")
        @Composable
        fun rememberMaxImageSize() =
            rememberPreference(key = MAX_IMAGE_SIZE, defaultValue = 4096)
    }

    object Search {
        private val HISTORY = stringSetPreferencesKey("search_history")

        @Composable
        fun rememberSearchHistory() =
            rememberPreference(key = HISTORY, defaultValue = emptySet())
    }

    object Misc {
        private val USER_CHOICE_MEDIA_MANAGER = booleanPreferencesKey("use_media_manager")
        @Composable
        fun rememberIsMediaManager() =
            rememberPreference(key = USER_CHOICE_MEDIA_MANAGER, defaultValue = false)

        private val ENABLE_TRASH = booleanPreferencesKey("enable_trashcan")
        @Composable
        fun rememberTrashEnabled() =
            rememberPreference(key = ENABLE_TRASH, defaultValue = true)
        fun getTrashEnabled(context: Context) =
            context.dataStore.data.map { it[ENABLE_TRASH] ?: true }

        private val LAST_SCREEN = stringPreferencesKey("last_screen")
        @Composable
        fun rememberLastScreen() =
            rememberPreference(key = LAST_SCREEN, defaultValue = Screen.TimelineScreen.route)

        private val MEDIA_GRID_SIZE = floatPreferencesKey("media_grid_size")
        @Composable
        fun rememberMediaGridSize() =
            rememberPreference(key = MEDIA_GRID_SIZE, defaultValue = Dimens.Photo().value)

        private val ALBUM_GRID_SIZE = floatPreferencesKey("album_grid_size")
        @Composable
        fun rememberAlbumGridSize() =
            rememberPreference(key = ALBUM_GRID_SIZE, defaultValue = Dimens.Album().value)

        private val FORCE_THEME = booleanPreferencesKey("force_theme")

        @Composable
        fun rememberForceTheme() =
            rememberPreference(key = FORCE_THEME, defaultValue = false)

        private val DARK_MODE = booleanPreferencesKey("dark_mode")

        @Composable
        fun rememberIsDarkMode() =
            rememberPreference(key = DARK_MODE, defaultValue = false)

        private val AMOLED_MODE = booleanPreferencesKey("amoled_mode")

        @Composable
        fun rememberIsAmoledMode() =
            rememberPreference(key = AMOLED_MODE, defaultValue = false)

        private val SECURE_MODE = booleanPreferencesKey("secure_mode")

        @Composable
        fun rememberSecureMode() =
            rememberPreference(key = SECURE_MODE, defaultValue = true)

        fun getSecureMode(context: Context) =
            context.dataStore.data.map { it[SECURE_MODE] ?: true }

        private val TIMELINE_GROUP_BY_MONTH = booleanPreferencesKey("timeline_group_by_month")

        @Composable
        fun rememberTimelineGroupByMonth() =
            rememberPreference(key = TIMELINE_GROUP_BY_MONTH, defaultValue = false)

    }
}

sealed class SettingsType {
    object Seek : SettingsType()
    object Switch : SettingsType()
    object Header : SettingsType()
    object Default : SettingsType()
}

sealed class Position {
    object Top : Position()
    object Middle : Position()
    object Bottom : Position()
    object Alone : Position()
}

sealed class SettingsEntity(
    open val icon: ImageVector? = null,
    open val title: String,
    open val summary: String? = null,
    val type: SettingsType = SettingsType.Default,
    open val enabled: Boolean = true,
    open val isChecked: Boolean? = null,
    open val onCheck: ((Boolean) -> Unit)? = null,
    open val onClick: (() -> Unit)? = null,
    open val minValue: Float? = null,
    open val currentValue: Float? = null,
    open val maxValue: Float? = null,
    open val step: Int = 1,
    open val valueMultiplier: Int = 1,
    open val seekSuffix: String? = null,
    open val onSeek: ((Float) -> Unit)? = null,
    open val screenPosition: Position = Position.Alone
) {
    val isHeader = type == SettingsType.Header

    data class Header(
        override val title: String
    ): SettingsEntity(
        title = title,
        type = SettingsType.Header
    )

    data class Preference(
        override val icon: ImageVector? = null,
        override val title: String,
        override val summary: String? = null,
        override val enabled: Boolean = true,
        override val screenPosition: Position = Position.Alone,
        override val onClick: (() -> Unit)? = null,
    ) : SettingsEntity(
        icon = icon,
        title = title,
        summary = summary,
        enabled = enabled,
        screenPosition = screenPosition,
        onClick = onClick,
        type = SettingsType.Default
    )

    data class SwitchPreference(
        override val icon: ImageVector? = null,
        override val title: String,
        override val summary: String? = null,
        override val enabled: Boolean = true,
        override val screenPosition: Position = Position.Alone,
        override val isChecked: Boolean = false,
        override val onCheck: ((Boolean) -> Unit)? = null,
    ): SettingsEntity(
        icon = icon,
        title = title,
        summary = summary,
        enabled = enabled,
        isChecked = isChecked,
        onCheck = onCheck,
        screenPosition = screenPosition,
        type = SettingsType.Switch
    )

    data class SeekPreference(
        override val icon: ImageVector? = null,
        override val title: String,
        override val summary: String? = null,
        override val enabled: Boolean = true,
        override val screenPosition: Position = Position.Alone,
        override val minValue: Float? = null,
        override val currentValue: Float? = null,
        override val maxValue: Float? = null,
        override val step: Int = 1,
        override val valueMultiplier: Int = 1,
        override val seekSuffix: String? = null,
        override val onSeek: ((Float) -> Unit)? = null,
    ): SettingsEntity(
        icon = icon,
        title = title,
        summary = summary,
        enabled = enabled,
        screenPosition = screenPosition,
        minValue = minValue,
        currentValue = currentValue,
        maxValue = maxValue,
        step = step,
        valueMultiplier = valueMultiplier,
        seekSuffix = seekSuffix,
        onSeek = onSeek,
        type = SettingsType.Seek
    )
}