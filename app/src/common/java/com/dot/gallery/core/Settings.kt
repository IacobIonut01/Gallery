/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class Settings(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var albumLastSort: Int
        get() = sharedPreferences.getInt(Album.LAST_SORT, 0)
        set(value) = sharedPreferences
            .edit()
            .putInt(Album.LAST_SORT, value)
            .apply()

    var useMediaManager: Boolean
        get() = sharedPreferences.getBoolean(Misc.USER_CHOICE_MEDIA_MANAGER, false)
        set(value) = sharedPreferences
            .edit()
            .putBoolean(Misc.USER_CHOICE_MEDIA_MANAGER, value)
            .apply()

    var trashCanEnabled: Boolean
        get() = sharedPreferences.getBoolean(Misc.ENABLE_TRASH, true)
        set(value) = sharedPreferences
            .edit()
            .putBoolean(Misc.ENABLE_TRASH, value)
            .apply()

    var diskCacheSize: Long
        get() = sharedPreferences.getLong(Glide.DISK_CACHE_SIZE, 150)
        set(value) = sharedPreferences
            .edit()
            .putLong(Glide.DISK_CACHE_SIZE, value)
            .apply()

    var cachedScreenCount: Float
        get() = sharedPreferences.getFloat(Glide.CACHED_SCREEN_COUNT, 8f)
        set(value) = sharedPreferences
            .edit()
            .putFloat(Glide.CACHED_SCREEN_COUNT, value)
            .apply()

    fun resetToDefaults() {
        albumLastSort = 0
        useMediaManager = false
        trashCanEnabled = true
    }

    object Album {
        const val LAST_SORT = "album_last_sort"
    }

    object Glide {
        const val DISK_CACHE_SIZE = "disk_cache_size"
        const val CACHED_SCREEN_COUNT = "cached_screen_count"
    }

    object Misc {
        const val USER_CHOICE_MEDIA_MANAGER = "use_media_manager"
        const val ENABLE_TRASH = "enable_trashcan"
    }
    companion object {
        private const val PREFERENCE_NAME = "settings"
    }
}

sealed class SettingsType {
    object Switch: SettingsType()
    object Header: SettingsType()
    object Default: SettingsType()
}

data class SettingsEntity(
    val icon: ImageVector? = null,
    val title: String,
    val summary: String? = null,
    val type: SettingsType = SettingsType.Default,
    val enabled: Boolean = true,
    val isChecked: Boolean? = null,
    val onCheck: ((Boolean) -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)