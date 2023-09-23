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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dot.gallery.core.Settings.PREFERENCE_NAME
import com.dot.gallery.core.util.rememberPreference
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.theme.Dimens
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

object Settings {

    const val PREFERENCE_NAME = "settings"

    object Album {
        private val LAST_SORT = intPreferencesKey("album_last_sort")
        @Composable
        fun rememberLastSort() =
            rememberPreference(key = LAST_SORT, defaultValue = 0)

        private val ALBUM_SIZE = floatPreferencesKey("album_size")

        @Composable
        fun rememberAlbumSize() =
            rememberPreference(key = ALBUM_SIZE, defaultValue = Dimens.Album.size.value)
    }

    object Glide {
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
            rememberPreference(key = SECURE_MODE, defaultValue = false)

        fun getSecureMode(context: Context) =
            context.dataStore.data.map { it[SECURE_MODE] ?: false }

        private val TIMELINE_GROUP_BY_MONTH = booleanPreferencesKey("timeline_group_by_month")

        @Composable
        fun rememberTimelineGroupByMonth() =
            rememberPreference(key = TIMELINE_GROUP_BY_MONTH, defaultValue = false)

        private val ALLOW_BLUR = booleanPreferencesKey("allow_blur")

        @Composable
        fun rememberAllowBlur() = rememberPreference(key = ALLOW_BLUR, defaultValue = true)
    }
}

sealed class SettingsType {
    data object Seek : SettingsType()
    data object Switch : SettingsType()
    data object Header : SettingsType()
    data object Default : SettingsType()
}

sealed class Position {
    data object Top : Position()
    data object Middle : Position()
    data object Bottom : Position()
    data object Alone : Position()
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