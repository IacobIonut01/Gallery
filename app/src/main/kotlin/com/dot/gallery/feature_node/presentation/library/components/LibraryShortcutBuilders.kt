/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.core.Settings
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.feature_node.domain.model.LibraryIndicatorState
import com.dot.gallery.feature_node.presentation.library.CloudLibraryState
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.ui.core.icons.Encrypted
import com.dot.gallery.ui.core.Icons as GalleryIcons

/**
 * Build the live runtime definition for every potentially-available Library
 * shortcut, keyed by [LibraryShortcut]. The set returned only includes
 * shortcuts whose underlying feature is currently usable.
 *
 * Used by both the Library screen (render) and the customization editor.
 */
@Composable
fun rememberLibraryRuntimeShortcuts(
    indicatorState: LibraryIndicatorState,
    cloudState: CloudLibraryState,
): Map<LibraryShortcut, RuntimeShortcut> {
    val privateFolderUri by Settings.Security.rememberPrivateFolderUri()
    val colorScheme = MaterialTheme.colorScheme
    val map = LinkedHashMap<LibraryShortcut, RuntimeShortcut>()

    if (SdkCompat.supportsTrash) {
        map[LibraryShortcut.TRASH] = RuntimeShortcut(
            shortcut = LibraryShortcut.TRASH,
            title = stringResource(R.string.trash),
            icon = Icons.Outlined.DeleteOutline,
            contentColor = colorScheme.primary,
            useIndicator = true,
            indicatorCounter = indicatorState.trashCount,
            route = Screen.TrashedScreen.route,
            available = true
        )
    }
    if (SdkCompat.supportsFavorites) {
        map[LibraryShortcut.FAVORITES] = RuntimeShortcut(
            shortcut = LibraryShortcut.FAVORITES,
            title = stringResource(R.string.favorites),
            icon = Icons.Outlined.FavoriteBorder,
            contentColor = colorScheme.error,
            useIndicator = true,
            indicatorCounter = indicatorState.favoriteCount,
            route = Screen.FavoriteScreen.route,
            available = true
        )
    }
    map[LibraryShortcut.VAULT] = RuntimeShortcut(
        shortcut = LibraryShortcut.VAULT,
        title = stringResource(R.string.vault),
        icon = GalleryIcons.Encrypted,
        contentColor = colorScheme.secondary,
        useIndicator = false,
        indicatorCounter = 0,
        route = Screen.VaultScreen.route,
        available = true
    )
    map[LibraryShortcut.IGNORED] = RuntimeShortcut(
        shortcut = LibraryShortcut.IGNORED,
        title = stringResource(R.string.ignored),
        icon = Icons.Outlined.VisibilityOff,
        contentColor = colorScheme.tertiary,
        useIndicator = false,
        indicatorCounter = 0,
        route = Screen.IgnoredScreen.route,
        available = true
    )
    if (privateFolderUri.isNotEmpty()) {
        map[LibraryShortcut.PRIVATE_FOLDER] = RuntimeShortcut(
            shortcut = LibraryShortcut.PRIVATE_FOLDER,
            title = stringResource(R.string.security_private_folder),
            icon = Icons.Outlined.FolderOff,
            contentColor = colorScheme.secondary,
            useIndicator = false,
            indicatorCounter = 0,
            route = Screen.PrivateFolderScreen.route,
            available = true
        )
    }
    if (cloudState.hasCloud) {
        if (cloudState.hasArchive) {
            map[LibraryShortcut.CLOUD_ARCHIVE] = RuntimeShortcut(
                shortcut = LibraryShortcut.CLOUD_ARCHIVE,
                title = stringResource(R.string.cloud_archive),
                icon = Icons.Outlined.Archive,
                contentColor = colorScheme.secondary,
                useIndicator = true,
                indicatorCounter = cloudState.archivedCount,
                route = Screen.CloudArchiveScreen.route,
                available = true
            )
        }
        if (cloudState.hasShareLink) {
            map[LibraryShortcut.CLOUD_SHARED_LINKS] = RuntimeShortcut(
                shortcut = LibraryShortcut.CLOUD_SHARED_LINKS,
                title = stringResource(R.string.cloud_shared_links),
                icon = Icons.Outlined.Link,
                contentColor = colorScheme.tertiary,
                useIndicator = true,
                indicatorCounter = cloudState.sharedLinkCount,
                route = Screen.SharedLinksScreen.route,
                available = true
            )
        }
        map[LibraryShortcut.CLOUD_BACKUP] = RuntimeShortcut(
            shortcut = LibraryShortcut.CLOUD_BACKUP,
            title = stringResource(R.string.cloud_backup_and_sync),
            icon = Icons.Outlined.Backup,
            contentColor = colorScheme.primary,
            useIndicator = false,
            indicatorCounter = 0,
            route = Screen.CloudBackupDashboardScreen.route,
            available = true
        )
        map[LibraryShortcut.CLOUD_ACCOUNTS] = RuntimeShortcut(
            shortcut = LibraryShortcut.CLOUD_ACCOUNTS,
            title = stringResource(R.string.cloud_accounts),
            icon = Icons.Outlined.Cloud,
            contentColor = colorScheme.onSurface,
            useIndicator = false,
            indicatorCounter = 0,
            route = Screen.CloudAccountsScreen.route,
            available = true
        )
    }
    return map
}
