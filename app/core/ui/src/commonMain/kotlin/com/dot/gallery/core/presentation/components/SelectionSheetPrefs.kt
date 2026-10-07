/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.domain.settings.PrefKey
import com.dot.gallery.domain.settings.SettingsKeys

/** Persisted `selection_sheet_config` — JSON-encoded [SelectionSheetConfig]. */
val selectionSheetConfigKey = PrefKey.serializable(
    name = "selection_sheet_config",
    default = SelectionSheetConfig(),
    serializer = SelectionSheetConfig.serializer(),
)

@Composable
fun rememberSelectionSheetConfig(): MutableState<SelectionSheetConfig> =
    rememberPreference(key = selectionSheetConfigKey)

@Composable
fun rememberShowSelectionTitles(): MutableState<Boolean> =
    rememberPreference(key = SettingsKeys.selectionTitles)
