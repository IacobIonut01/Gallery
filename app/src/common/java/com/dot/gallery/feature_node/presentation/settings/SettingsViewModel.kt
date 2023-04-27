/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings

import android.content.Context
import com.dot.gallery.BuildConfig
import com.dot.gallery.R
import com.dot.gallery.core.Settings
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.SettingsType.Default
import com.dot.gallery.core.SettingsType.Header
import com.dot.gallery.core.SettingsType.Switch
import com.dot.gallery.feature_node.presentation.ChanneledViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settings: Settings
) : ChanneledViewModel() {

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
                type = Default,
                title = context.getString(R.string.settings_about_app_title),
                summary = context.getString(R.string.settings_about_app_summary, BuildConfig.VERSION_NAME)
            )
        )
}