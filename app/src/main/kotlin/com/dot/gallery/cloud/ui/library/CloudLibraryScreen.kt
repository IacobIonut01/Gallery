/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.cloud.core.PersonInfo
import com.dot.gallery.feature_node.presentation.library.components.LibrarySmallItem
import com.dot.gallery.feature_node.presentation.util.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudLibraryScreen(
    paddingValues: PaddingValues,
    navigate: (String) -> Unit
) {
    val viewModel = hiltViewModel<CloudLibraryViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .padding(
                start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
            )
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CloudLibraryTopBar(
                scrollBehavior = scrollBehavior,
                onSettingsClick = { navigate(Screen.CloudAccountsScreen.route) },
                onProfileClick = { navigate(Screen.CloudProfileScreen.route) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 128.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick actions row
            item(key = "quick_actions") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibrarySmallItem(
                            title = "Favorites",
                            icon = Icons.Outlined.FavoriteBorder,
                            contentColor = MaterialTheme.colorScheme.error,
                            useIndicator = true,
                            indicatorCounter = state.favoriteCount,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { navigate(Screen.FavoriteScreen.route) }
                        )
                        if (state.hasArchiveCapability) {
                            LibrarySmallItem(
                                title = "Archive",
                                icon = Icons.Outlined.Archive,
                                contentColor = MaterialTheme.colorScheme.secondary,
                                useIndicator = true,
                                indicatorCounter = state.archivedCount,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { navigate(Screen.CloudArchiveScreen.route) }
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibrarySmallItem(
                            title = "Trash",
                            icon = Icons.Outlined.DeleteOutline,
                            contentColor = MaterialTheme.colorScheme.primary,
                            useIndicator = true,
                            indicatorCounter = state.trashedCount,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { navigate(Screen.TrashedScreen.route) }
                        )
                        LibrarySmallItem(
                            title = "Backup & Sync",
                            icon = Icons.Outlined.Backup,
                            contentColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { navigate(Screen.CloudBackupAndSyncScreen.route) }
                        )
                    }
                }
            }

            // People section
            if (state.hasPeopleCapability && state.people.isNotEmpty()) {
                item(key = "people_header") {
                    SectionHeader(
                        title = "People",
                        count = state.people.size,
                        onClick = { navigate(Screen.PeopleListScreen.route) }
                    )
                }
                item(key = "people_row") {
                    PeopleRow(
                        people = state.people,
                        onPersonClick = { person ->
                            navigate(Screen.PersonDetailScreen.personId(person.id))
                        }
                    )
                }
            }

            // Utility links
            item(key = "utility_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.hasMapCapability) {
                        UtilityItem(
                            title = "Places",
                            icon = Icons.Outlined.LocationOn,
                            onClick = { navigate(Screen.CloudPlacesScreen.route) }
                        )
                    }
                    if (state.hasShareLinkCapability) {
                        UtilityItem(
                            title = "Shared Links",
                            subtitle = if (state.sharedLinks.isNotEmpty()) "${state.sharedLinks.size} links" else null,
                            icon = Icons.Outlined.Link,
                            onClick = { navigate(Screen.SharedLinksScreen.route) }
                        )
                    }
                    UtilityItem(
                        title = "Upload Settings",
                        icon = Icons.Outlined.Cloud,
                        onClick = { navigate(Screen.CloudUploadSettingsScreen.route) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudLibraryTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    LargeTopAppBar(
        title = {
            Text(
                text = "Cloud Library",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int = 0,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (count > 0) "$title ($count)" else title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "See all",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PeopleRow(
    people: List<PersonInfo>,
    onPersonClick: (PersonInfo) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = people.take(20),
            key = { it.id }
        ) { person ->
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onPersonClick(person) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.People,
                    contentDescription = person.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = person.name.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UtilityItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    LibrarySmallItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
}
