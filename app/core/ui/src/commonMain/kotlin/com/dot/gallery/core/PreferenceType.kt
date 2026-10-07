/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core


sealed class PreferenceType {
    data object Seek : PreferenceType()
    data object Switch : PreferenceType()
    data object Header : PreferenceType()
    data object Default : PreferenceType()
    data object Album : PreferenceType()
    data object Log : PreferenceType()
}
