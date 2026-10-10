/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dot.gallery.core.presentation.util.NavigationItem

/**
 * The classic navigation rail used on wide windows. Pure rendering extracted
 * unchanged from the app's private `ClassicNavigationRail`.
 */
@Composable
fun ClassicNavigationRailContent(
    selectedRoute: String?,
    navigationItems: List<NavigationItem>,
    onClick: (route: String) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(Modifier.weight(1f))
        navigationItems.forEach { item ->
            val selected = item.route == selectedRoute
            NavigationRailItem(
                selected = selected,
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                onClick = { onClick(item.route) },
                label = { NavigationItemLabel(item) },
                icon = { NavigationItemIcon(item) }
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
