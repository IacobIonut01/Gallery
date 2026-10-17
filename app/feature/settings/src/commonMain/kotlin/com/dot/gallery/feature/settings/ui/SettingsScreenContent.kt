/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.settings.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.ui.components.BaseSettingsScreen
import com.dot.gallery.core.ui.components.CustomCircleIcon
import com.dot.gallery.core.ui.components.SettingsAppHeader
import com.dot.gallery.core.ui.components.SettingsAppHeaderCompact
import com.dot.gallery.core.ui.components.SettingsItem
import com.dot.gallery.core.ui.components.rememberPreference
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.settings.generated.resources.Res
import com.dot.gallery.feature.settings.generated.resources.ai_category
import com.dot.gallery.feature.settings.generated.resources.ai_category_summary
import com.dot.gallery.feature.settings.generated.resources.dev_menu_summary
import com.dot.gallery.feature.settings.generated.resources.dev_menu_title
import com.dot.gallery.feature.settings.generated.resources.help_summary
import com.dot.gallery.feature.settings.generated.resources.help_title
import com.dot.gallery.feature.settings.generated.resources.settings_appearance
import com.dot.gallery.feature.settings.generated.resources.settings_appearance_summary
import com.dot.gallery.feature.settings.generated.resources.settings_backup
import com.dot.gallery.feature.settings.generated.resources.settings_backup_summary
import com.dot.gallery.feature.settings.generated.resources.settings_cloud_accounts
import com.dot.gallery.feature.settings.generated.resources.settings_cloud_accounts_summary
import com.dot.gallery.feature.settings.generated.resources.settings_general
import com.dot.gallery.feature.settings.generated.resources.settings_general_summary
import com.dot.gallery.feature.settings.generated.resources.settings_media_viewer
import com.dot.gallery.feature.settings.generated.resources.settings_media_viewer_summary
import com.dot.gallery.feature.settings.generated.resources.settings_navigation
import com.dot.gallery.feature.settings.generated.resources.settings_navigation_summary
import com.dot.gallery.feature.settings.generated.resources.settings_security
import com.dot.gallery.feature.settings.generated.resources.settings_security_summary
import com.dot.gallery.feature.settings.generated.resources.settings_timeline_albums
import com.dot.gallery.feature.settings.generated.resources.settings_timeline_albums_summary
import com.dot.gallery.feature.settings.generated.resources.settings_title

/**
 * Settings hub — whole screen, state + callbacks only. The platform wrapper
 * owns preferences ([state]), navigation ([actions]), the support sheet, and
 * the developer-unlock toast.
 */
@Composable
fun SettingsScreenContent(
    state: SettingsScreenState,
    actions: SettingsScreenActions,
) {
    @Composable
    fun rememberDashboardSettings(): SnapshotStateList<SettingsEntity> {
        val appearancePref = rememberPreference(
            icon = Icons.Outlined.Palette,
            title = stringResource(Res.string.settings_appearance),
            summary = stringResource(Res.string.settings_appearance_summary),
            onClick = { actions.onNavigate(SettingsRoute.APPEARANCE) },
            screenPosition = Position.Top
        )
        val timelineAlbumsPref = rememberPreference(
            icon = Icons.Outlined.GridView,
            title = stringResource(Res.string.settings_timeline_albums),
            summary = stringResource(Res.string.settings_timeline_albums_summary),
            onClick = { actions.onNavigate(SettingsRoute.TIMELINE_ALBUMS) },
            screenPosition = Position.Middle
        )
        val mediaViewerPref = rememberPreference(
            icon = Icons.Outlined.Fullscreen,
            title = stringResource(Res.string.settings_media_viewer),
            summary = stringResource(Res.string.settings_media_viewer_summary),
            onClick = { actions.onNavigate(SettingsRoute.MEDIA_VIEWER) },
            screenPosition = Position.Middle
        )
        val navigationPref = rememberPreference(
            icon = Icons.Outlined.Explore,
            title = stringResource(Res.string.settings_navigation),
            summary = stringResource(Res.string.settings_navigation_summary),
            onClick = { actions.onNavigate(SettingsRoute.NAVIGATION) },
            screenPosition = Position.Middle
        )
        val generalPref = rememberPreference(
            icon = Icons.Outlined.Dashboard,
            title = stringResource(Res.string.settings_general),
            summary = stringResource(Res.string.settings_general_summary),
            onClick = { actions.onNavigate(SettingsRoute.GENERAL) },
            screenPosition = Position.Middle
        )
        val securityPref = rememberPreference(
            icon = Icons.Outlined.Shield,
            title = stringResource(Res.string.settings_security),
            summary = stringResource(Res.string.settings_security_summary),
            onClick = { actions.onNavigate(SettingsRoute.SECURITY) },
            screenPosition = Position.Middle
        )
        val backupPref = rememberPreference(
            icon = Icons.Outlined.Backup,
            title = stringResource(Res.string.settings_backup),
            summary = stringResource(Res.string.settings_backup_summary),
            onClick = { actions.onNavigate(SettingsRoute.BACKUP) },
            screenPosition = Position.Middle
        )
        val cloudPref = if (state.hasCloudProviders) rememberPreference(
            icon = Icons.Outlined.Cloud,
            title = stringResource(Res.string.settings_cloud_accounts),
            summary = stringResource(Res.string.settings_cloud_accounts_summary),
            onClick = { actions.onNavigate(SettingsRoute.CLOUD_ACCOUNTS) },
            screenPosition = Position.Middle
        ) else null
        val smartPref = rememberPreference(
            icon = Icons.Outlined.SettingsSuggest,
            title = stringResource(Res.string.ai_category),
            summary = stringResource(Res.string.ai_category_summary),
            onClick = { actions.onNavigate(SettingsRoute.SMART_FEATURES) },
            screenPosition = Position.Middle
        )
        val helpPref = rememberPreference(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            title = stringResource(Res.string.help_title),
            summary = stringResource(Res.string.help_summary),
            onClick = { actions.onNavigate(SettingsRoute.HELP) },
            screenPosition = Position.Middle
        )
        val developerPref = rememberPreference(
            icon = Icons.Outlined.BugReport,
            title = stringResource(Res.string.dev_menu_title),
            summary = stringResource(Res.string.dev_menu_summary),
            onClick = { actions.onNavigate(SettingsRoute.DEVELOPER) },
            screenPosition = Position.Bottom
        )
        return remember(
            appearancePref, timelineAlbumsPref, mediaViewerPref,
            navigationPref, generalPref, securityPref, backupPref, cloudPref, smartPref, helpPref,
            developerPref
        ) {
            mutableStateListOf<SettingsEntity>(
                appearancePref, timelineAlbumsPref, mediaViewerPref,
                navigationPref, generalPref, securityPref, backupPref,
            ).apply {
                if (cloudPref != null) add(cloudPref)
                addAll(listOf(smartPref, helpPref, developerPref))
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val tertiaryContainerColor = MaterialTheme.colorScheme.tertiaryContainer
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val inversePrimaryColor = MaterialTheme.colorScheme.inversePrimary
    val backgroundColors = remember(
        primaryColor, secondaryColor, tertiaryColor,
        errorColor, primaryContainerColor, secondaryContainerColor, tertiaryContainerColor, surfaceVariantColor,
        inversePrimaryColor
    ) {
        listOf(
            primaryColor, secondaryColor, inversePrimaryColor, tertiaryColor,
            errorColor, primaryContainerColor, secondaryContainerColor, tertiaryContainerColor, surfaceVariantColor
        )
    }
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val onSecondaryColor = MaterialTheme.colorScheme.onSecondary
    val onTertiaryColor = MaterialTheme.colorScheme.onTertiary
    val onErrorColor = MaterialTheme.colorScheme.onError
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val onSecondaryContainerColor = MaterialTheme.colorScheme.onSecondaryContainer
    val onTertiaryContainerColor = MaterialTheme.colorScheme.onTertiaryContainer
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onBackgroundColors = remember(
        onPrimaryColor, onSecondaryColor, onTertiaryColor,
        onErrorColor, onPrimaryContainerColor, onSecondaryContainerColor, onTertiaryContainerColor, onSurfaceVariantColor,
        onSurfaceColor
    ) {
        listOf(
            onPrimaryColor, onSecondaryColor, onSurfaceColor, onTertiaryColor,
            onErrorColor, onPrimaryContainerColor, onSecondaryContainerColor, onTertiaryContainerColor, onSurfaceVariantColor
        )
    }

    BaseSettingsScreen(
        title = stringResource(Res.string.settings_title),
        topContent = if (!state.bannerDismissed) {
            {
                SettingsAppHeader(
                    onDismiss = actions.onDismissBanner,
                    onSupportClick = actions.onShowSupport,
                    developerModeEnabled = state.developerMode,
                    onDeveloperUnlocked = actions.onDeveloperUnlocked,
                )
            }
        } else null,
        bottomContent = if (state.bannerDismissed) {
            {
                SettingsAppHeaderCompact(
                    onRestore = actions.onRestoreBanner,
                    onSupportClick = actions.onShowSupport,
                    developerModeEnabled = state.developerMode,
                    onDeveloperUnlocked = actions.onDeveloperUnlocked,
                )
            }
        } else null,
        settingsList = rememberDashboardSettings(),
        settingsBuilder = { setting, index, modifier ->
            SettingsItem(
                item = setting,
                modifier = modifier,
                customIcon = { icon, iconUri, iconRes ->
                    CustomCircleIcon(
                        iconVector = icon,
                        iconUri = iconUri,
                        iconRes = iconRes,
                        containerColor = backgroundColors[index % backgroundColors.size],
                        contentColor = onBackgroundColors[index % onBackgroundColors.size]
                    )
                }
            )
        }
    )
}
