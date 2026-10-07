/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import com.dot.gallery.core.location.MapAppearance
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.domain.settings.SettingsKeys

/** String-stored map-appearance pref surfaced as a typed [MutableState]. */
@Composable
fun rememberMapAppearance(): MutableState<MapAppearance> {
    val stored = rememberPreference(
        key = SettingsKeys.mapAppearance
    )
    return remember(stored) {
        object : MutableState<MapAppearance> {
            override var value: MapAppearance
                get() = MapAppearance.fromStored(stored.value)
                set(value) {
                    stored.value = value.storedValue
                }

            override fun component1(): MapAppearance = value
            override fun component2(): (MapAppearance) -> Unit = { value = it }
        }
    }
}
