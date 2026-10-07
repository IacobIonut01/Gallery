/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/**
 * A single DataStore preference value, tagged with its primitive type so it can be
 * faithfully reconstructed on import.
 *
 * [type] codes: b=Boolean, i=Int, l=Long, f=Float, d=Double, s=String, ss=Set<String>.
 * For `ss`, [value] holds a JSON-encoded list of strings.
 */
@Serializable
data class SettingValue(
    val type: String,
    val value: String
)
