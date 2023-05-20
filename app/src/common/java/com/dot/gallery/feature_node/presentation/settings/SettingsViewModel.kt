/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.Settings.Glide.rememberCachedScreenCount
import com.dot.gallery.core.Settings.Glide.rememberDiskCacheSize
import com.dot.gallery.core.Settings.Misc.rememberForceTheme
import com.dot.gallery.core.Settings.Misc.rememberIsDarkMode
import com.dot.gallery.core.Settings.Misc.rememberTrashEnabled
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.SettingsEntity.Header
import com.dot.gallery.core.SettingsEntity.SeekPreference
import com.dot.gallery.core.SettingsEntity.SwitchPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    @Composable
    fun rememberSettingsList(): SnapshotStateList<SettingsEntity> {
        val context = LocalContext.current
        var forceTheme by rememberForceTheme()
        val forceThemeValuePref = remember(forceTheme) {
            SwitchPreference(
                title = context.getString(R.string.settings_follow_system_theme_title),
                isChecked = !forceTheme,
                onCheck = { forceTheme = !it },
                screenPosition = Position.Top
            )
        }
        var darkModeValue by rememberIsDarkMode()
        val darkThemePref = remember(darkModeValue, forceTheme) {
            SwitchPreference(
                title = context.getString(R.string.settings_dark_mode_title),
                enabled = forceTheme,
                isChecked = darkModeValue,
                onCheck = { darkModeValue = it },
                screenPosition = Position.Bottom
            )
        }
        var trashCanEnabled by rememberTrashEnabled()
        val trashCanEnabledPref = remember(trashCanEnabled) {
            SwitchPreference(
                title = context.getString(R.string.settings_trash_title),
                summary = context.getString(R.string.settings_trash_summary),
                isChecked = trashCanEnabled,
                onCheck = { trashCanEnabled = it }
            )
        }
        var diskCacheSize by rememberDiskCacheSize()
        val diskCachePref = remember(diskCacheSize) {
            SeekPreference(
                screenPosition = Position.Top,
                title = context.getString(R.string.settings_disk_cache_title),
                summary = context.getString(R.string.settings_disk_cache_summary),
                currentValue = diskCacheSize.toFloat(),
                minValue = 2f,
                maxValue = 50f,
                valueMultiplier = 10,
                step = 48,
                seekSuffix = context.getString(R.string.mb),
                onSeek = { diskCacheSize = it.roundToLong() }
            )
        }
        var cachedScreenCount by rememberCachedScreenCount()
        val cachedScreenCountPref = remember(cachedScreenCount) {
            SeekPreference(
                screenPosition = Position.Bottom,
                title = context.getString(R.string.settings_cached_screen_title),
                summary = context.getString(R.string.settings_cached_screen_summary),
                currentValue = cachedScreenCount,
                minValue = 1f,
                maxValue = 20f,
                step = 19,
                onSeek = { cachedScreenCount = it }
            )
        }

        return remember(arrayOf(trashCanEnabled, diskCacheSize, cachedScreenCount)) {
            mutableStateListOf<SettingsEntity>().apply {
                /** ********************* **/
                add(Header(title = context.getString(R.string.settings_theme_header)))
                /** Theme Section Start **/
                add(forceThemeValuePref)
                add(darkThemePref)
                /** ********************* **/
                add(Header(title = context.getString(R.string.settings_general)))
                /** General Section Start **/
                add(trashCanEnabledPref)
                /** General Section End **/

                /** ********************* **/
                add(Header(title = context.getString(R.string.settings_cache)))
                /** Cache Section Start **/
                add(diskCachePref)
                add(cachedScreenCountPref)
                /** Cache Section End **/
            }
        }
    }
}