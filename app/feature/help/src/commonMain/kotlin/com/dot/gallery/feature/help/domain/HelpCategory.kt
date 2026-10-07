/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.help.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.help.generated.resources.Res
import com.dot.gallery.feature.help.generated.resources.help_cat_accessibility
import com.dot.gallery.feature.help.generated.resources.help_cat_ai
import com.dot.gallery.feature.help.generated.resources.help_cat_albums
import com.dot.gallery.feature.help.generated.resources.help_cat_basics
import com.dot.gallery.feature.help.generated.resources.help_cat_cloud_sync
import com.dot.gallery.feature.help.generated.resources.help_cat_editing
import com.dot.gallery.feature.help.generated.resources.help_cat_fav_trash
import com.dot.gallery.feature.help.generated.resources.help_cat_gestures
import com.dot.gallery.feature.help.generated.resources.help_cat_locations
import com.dot.gallery.feature.help.generated.resources.help_cat_metadata
import com.dot.gallery.feature.help.generated.resources.help_cat_navigation
import com.dot.gallery.feature.help.generated.resources.help_cat_personalization
import com.dot.gallery.feature.help.generated.resources.help_cat_search
import com.dot.gallery.feature.help.generated.resources.help_cat_selection_actions
import com.dot.gallery.feature.help.generated.resources.help_cat_settings_appearance
import com.dot.gallery.feature.help.generated.resources.help_cat_settings_general
import com.dot.gallery.feature.help.generated.resources.help_cat_settings_navigation
import com.dot.gallery.feature.help.generated.resources.help_cat_settings_security
import com.dot.gallery.feature.help.generated.resources.help_cat_settings_smart
import com.dot.gallery.feature.help.generated.resources.help_cat_timeline_albums
import com.dot.gallery.feature.help.generated.resources.help_cat_vault
import com.dot.gallery.feature.help.generated.resources.help_cat_viewer_actions
import com.dot.gallery.feature.help.generated.resources.help_cat_viewer_settings
import com.dot.gallery.feature.help.generated.resources.help_cat_viewing
import com.dot.gallery.feature.help.generated.resources.help_whats_new

enum class HelpCategory {
    WHATS_NEW,
    GET_STARTED_BASICS,
    GET_STARTED_NAVIGATION,
    GET_STARTED_PERSONALIZATION,
    TIMELINE_ALBUMS,
    VIEWING,
    VIEWER_ACTIONS,
    VIEWER_SETTINGS,
    EDITING,
    SEARCH,
    AI_FEATURES,
    ALBUMS,
    VAULT,
    CLOUD_SYNC,
    FAVORITES_TRASH,
    LOCATIONS,
    METADATA,
    SETTINGS_APPEARANCE,
    SETTINGS_GENERAL,
    SETTINGS_NAVIGATION,
    SETTINGS_SMART,
    SETTINGS_SECURITY,
    GESTURES,
    SELECTION_ACTIONS,
    ACCESSIBILITY
}

@Composable
fun HelpCategory.displayTitle(): String = when (this) {
    HelpCategory.WHATS_NEW -> stringResource(Res.string.help_whats_new)
    HelpCategory.GET_STARTED_BASICS -> stringResource(Res.string.help_cat_basics)
    HelpCategory.GET_STARTED_NAVIGATION -> stringResource(Res.string.help_cat_navigation)
    HelpCategory.GET_STARTED_PERSONALIZATION -> stringResource(Res.string.help_cat_personalization)
    HelpCategory.TIMELINE_ALBUMS -> stringResource(Res.string.help_cat_timeline_albums)
    HelpCategory.VIEWING -> stringResource(Res.string.help_cat_viewing)
    HelpCategory.VIEWER_ACTIONS -> stringResource(Res.string.help_cat_viewer_actions)
    HelpCategory.VIEWER_SETTINGS -> stringResource(Res.string.help_cat_viewer_settings)
    HelpCategory.EDITING -> stringResource(Res.string.help_cat_editing)
    HelpCategory.SEARCH -> stringResource(Res.string.help_cat_search)
    HelpCategory.AI_FEATURES -> stringResource(Res.string.help_cat_ai)
    HelpCategory.ALBUMS -> stringResource(Res.string.help_cat_albums)
    HelpCategory.VAULT -> stringResource(Res.string.help_cat_vault)
    HelpCategory.CLOUD_SYNC -> stringResource(Res.string.help_cat_cloud_sync)
    HelpCategory.FAVORITES_TRASH -> stringResource(Res.string.help_cat_fav_trash)
    HelpCategory.LOCATIONS -> stringResource(Res.string.help_cat_locations)
    HelpCategory.METADATA -> stringResource(Res.string.help_cat_metadata)
    HelpCategory.SETTINGS_APPEARANCE -> stringResource(Res.string.help_cat_settings_appearance)
    HelpCategory.SETTINGS_GENERAL -> stringResource(Res.string.help_cat_settings_general)
    HelpCategory.SETTINGS_NAVIGATION -> stringResource(Res.string.help_cat_settings_navigation)
    HelpCategory.SETTINGS_SMART -> stringResource(Res.string.help_cat_settings_smart)
    HelpCategory.SETTINGS_SECURITY -> stringResource(Res.string.help_cat_settings_security)
    HelpCategory.GESTURES -> stringResource(Res.string.help_cat_gestures)
    HelpCategory.SELECTION_ACTIONS -> stringResource(Res.string.help_cat_selection_actions)
    HelpCategory.ACCESSIBILITY -> stringResource(Res.string.help_cat_accessibility)
}

fun HelpCategory.icon(): ImageVector = when (this) {
    HelpCategory.WHATS_NEW -> Icons.Outlined.NewReleases
    HelpCategory.GET_STARTED_BASICS -> Icons.AutoMirrored.Outlined.HelpOutline
    HelpCategory.GET_STARTED_NAVIGATION -> Icons.Outlined.Navigation
    HelpCategory.GET_STARTED_PERSONALIZATION -> Icons.Outlined.Palette
    HelpCategory.TIMELINE_ALBUMS -> Icons.Outlined.GridView
    HelpCategory.VIEWING -> Icons.Outlined.PlayCircleOutline
    HelpCategory.VIEWER_ACTIONS -> Icons.Outlined.SmartDisplay
    HelpCategory.VIEWER_SETTINGS -> Icons.Outlined.Tune
    HelpCategory.EDITING -> Icons.Outlined.Brush
    HelpCategory.SEARCH -> Icons.Outlined.Search
    HelpCategory.AI_FEATURES -> Icons.Outlined.AutoAwesome
    HelpCategory.ALBUMS -> Icons.Outlined.Collections
    HelpCategory.VAULT -> Icons.Outlined.Lock
    HelpCategory.CLOUD_SYNC -> Icons.Outlined.Cloud
    HelpCategory.FAVORITES_TRASH -> Icons.Outlined.FavoriteBorder
    HelpCategory.LOCATIONS -> Icons.Outlined.LocationOn
    HelpCategory.METADATA -> Icons.Outlined.Info
    HelpCategory.SETTINGS_APPEARANCE -> Icons.Outlined.Colorize
    HelpCategory.SETTINGS_GENERAL -> Icons.Outlined.Settings
    HelpCategory.SETTINGS_NAVIGATION -> Icons.Outlined.Explore
    HelpCategory.SETTINGS_SMART -> Icons.Outlined.AutoAwesome
    HelpCategory.SETTINGS_SECURITY -> Icons.Outlined.Shield
    HelpCategory.GESTURES -> Icons.Outlined.Swipe
    HelpCategory.SELECTION_ACTIONS -> Icons.Outlined.ChecklistRtl
    HelpCategory.ACCESSIBILITY -> Icons.Outlined.AccessibilityNew
}
