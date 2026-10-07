/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

import com.dot.gallery.domain.settings.ALIAS_GALLERY
import com.dot.gallery.domain.settings.ALIAS_GALLERY_MONO
import com.dot.gallery.domain.settings.ALIAS_MONO
import com.dot.gallery.domain.settings.ALIAS_REFRA

/**
 * Choices offered on the Looks & Feel page. The selected name/logo aliases themselves
 * are read and written through `SettingsKeys.appNameAlias`/`appLogoAlias`, so this state
 * only carries which aliases are offered — mirrored on `Settings.Misc.ALIAS_*` values.
 */
data class SetupLooksFeelState(
    val nameAliases: List<String> = listOf(ALIAS_REFRA, ALIAS_GALLERY),
    val logoAliases: List<String> = listOf(ALIAS_REFRA, ALIAS_GALLERY, ALIAS_MONO, ALIAS_GALLERY_MONO),
)
