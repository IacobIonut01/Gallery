/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.Settings
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.SettingsType.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settings: Settings
) : ViewModel() {

    fun settingsList(context: Context): List<SettingsEntity> =
        listOf(
            SettingsEntity(
                type = Header,
                title = context.getString(R.string.settings_general)
            ),
            SettingsEntity(
                type = Switch,
                title = context.getString(R.string.settings_trash_title),
                summary = context.getString(R.string.settings_trash_summary),
                isChecked = settings.trashCanEnabled,
                onCheck = { settings.trashCanEnabled = it }
            ),
            SettingsEntity(
                type = Header,
                title = context.getString(R.string.settings_cache)
            ),
            SettingsEntity(
                type = Seek,
                screenPosition = Position.Top,
                title = context.getString(R.string.settings_disk_cache_title),
                summary = context.getString(R.string.settings_disk_cache_summary),
                currentValue = settings.diskCacheSize.toFloat(),
                minValue = 2f,
                maxValue = 20f,
                valueMultiplier = 10,
                step = 18,
                seekSuffix = context.getString(R.string.mb),
                onSeek = { settings.diskCacheSize = it.roundToLong() }
            ),
            SettingsEntity(
                type = Seek,
                screenPosition = Position.Bottom,
                title = context.getString(R.string.settings_cached_screen_title),
                summary = context.getString(R.string.settings_cached_screen_summary),
                currentValue = settings.cachedScreenCount,
                minValue = 1f,
                maxValue = 20f,
                step = 19,
                onSeek = { settings.cachedScreenCount = it }
            )
        )
}