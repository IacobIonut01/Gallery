/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.util.LocalHazeState
import com.dot.gallery.core.presentation.util.NavigationItem
import com.dot.gallery.core.presentation.util.hazeEffectScaled
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.core.ui.util.supportsBlur
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.ui.theme.ComponentSize
import com.dot.gallery.ui.theme.Spacing
import dev.chrisbanes.haze.blur.LocalHazeBlurStyle

/**
 * The floating Material-style bottom bar. Pure rendering — the caller supplies
 * the selected route and handles clicks. Extracted verbatim from the app's
 * `GalleryNavBar` so docs previews render the same bar without a NavController.
 */
@Composable
fun GalleryNavigationBarContent(
    modifier: Modifier = Modifier,
    selectedRoute: String?,
    navigationItems: List<NavigationItem>,
    onClick: (route: String) -> Unit,
) {
    val allowBlur by rememberPreference(SettingsKeys.allowBlur.withDefault(supportsBlur))
    val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    val navigationShape = MaterialTheme.shapes.extraLarge
    val backgroundModifier = remember(allowBlur, surfaceColor, navigationShape) {
        if (!allowBlur) {
            Modifier.background(
                color = surfaceColor,
                shape = navigationShape,
            )
        } else {
            Modifier
        }
    }
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.ExtraLarge)
            .then(modifier)
            .height(ComponentSize.NavigationBarHeight)
            .clip(navigationShape)
            .then(backgroundModifier)
            .hazeEffectScaled(
                state = LocalHazeState.current,
                style = LocalHazeBlurStyle.current
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationItems.forEach { item ->
            val selected = item.route == selectedRoute
            GalleryNavBarItem(
                navItem = item,
                isSelected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
fun RowScope.GalleryNavBarItem(
    navItem: NavigationItem,
    isSelected: Boolean,
    onClick: (route: String) -> Unit,
) {
    val selectedColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "selectedColor"
    )
    val selectedIconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "selectedIconColor"
    )
    Box(
        modifier = Modifier
            .height(ComponentSize.NavigationBarHeight)
            .weight(1f)
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                onClick = { onClick(navItem.route) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(ComponentSize.NavigationBarHeight)
                .background(
                    color = selectedColor,
                    shape = RoundedCornerShape(percent = 100),
                ),
        )
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = navItem.icon,
            contentDescription = navItem.name,
            tint = selectedIconColor,
        )
    }
}
