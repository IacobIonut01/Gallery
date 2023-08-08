/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.Settings.Misc.rememberForceTheme
import com.dot.gallery.core.Settings.Misc.rememberIsAmoledMode
import com.dot.gallery.core.Settings.Misc.rememberIsDarkMode
import com.dot.gallery.core.Settings.Misc.rememberSecureMode
import com.dot.gallery.core.Settings.Misc.rememberTimelineGroupByMonth
import com.dot.gallery.core.Settings.Misc.rememberTrashEnabled
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.SettingsEntity.Header
import com.dot.gallery.core.SettingsEntity.Preference
import com.dot.gallery.core.SettingsEntity.SwitchPreference
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.restartApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    @Composable
    fun rememberSettingsList(navigate: (String) -> Unit): SnapshotStateList<SettingsEntity> {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
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
                screenPosition = Position.Middle
            )
        }
        var amoledModeValue by rememberIsAmoledMode()
        val amoledModePref = remember(amoledModeValue) {
            SwitchPreference(
                title = context.getString(R.string.amoled_mode_title),
                summary = context.getString(R.string.amoled_mode_summary),
                isChecked = amoledModeValue,
                onCheck = { amoledModeValue = it },
                screenPosition = Position.Bottom
            )
        }
        var trashCanEnabled by rememberTrashEnabled()
        val trashCanEnabledPref = remember(trashCanEnabled) {
            SwitchPreference(
                title = context.getString(R.string.settings_trash_title),
                summary = context.getString(R.string.settings_trash_summary),
                isChecked = trashCanEnabled,
                onCheck = { trashCanEnabled = it },
                screenPosition = Position.Top
            )
        }
        var secureMode by rememberSecureMode()
        val secureModePref = remember(secureMode) {
            SwitchPreference(
                title = context.getString(R.string.secure_mode_title),
                summary = context.getString(R.string.secure_mode_summary),
                isChecked = secureMode,
                onCheck = { secureMode = it },
                screenPosition = Position.Bottom
            )
        }

        val albumSizePref = remember {
            Preference(
                title = context.getString(R.string.album_card_size_title),
                summary = context.getString(R.string.album_card_size_summary),
                screenPosition = Position.Top
            ) { navigate(Screen.AlbumSizeScreen.route) }
        }

        var groupByMonth by rememberTimelineGroupByMonth()
        val groupByMonthPref = remember(groupByMonth) {
            SwitchPreference(
                title = context.getString(R.string.monthly_timeline_title),
                summary = context.getString(R.string.monthly_timeline_summary),
                isChecked = groupByMonth,
                onCheck = {
                    scope.launch {
                        scope.async { groupByMonth = it }.await()
                        delay(50)
                        context.restartApplication()
                    }
                },
                screenPosition = Position.Bottom
            )
        }

        return remember(arrayOf(forceTheme, darkModeValue, trashCanEnabled, groupByMonth, amoledModeValue, secureMode)) {
            mutableStateListOf<SettingsEntity>().apply {
                /** ********************* **/
                add(Header(title = context.getString(R.string.settings_theme_header)))
                /** Theme Section Start **/
                add(forceThemeValuePref)
                add(darkThemePref)
                add(amoledModePref)
                /** ********************* **/
                add(Header(title = context.getString(R.string.customization)))
                /** Customization Section Start **/
                add(albumSizePref)
                add(groupByMonthPref)
                /** ********************* **/
                add(Header(title = context.getString(R.string.settings_general)))
                /** General Section Start **/
                add(trashCanEnabledPref)
                add(secureModePref)
                /** General Section End **/
            }
        }
    }
}