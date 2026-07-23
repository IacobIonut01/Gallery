package com.dot.gallery.feature_node.presentation.location

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R

@Composable
internal fun MapAppearanceButton(
    appearance: MapAppearance,
    onAppearanceChange: (MapAppearance) -> Unit,
    modifier: Modifier = Modifier,
    paddingModifier: Modifier = Modifier.padding(horizontal = 8.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    containerModifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .then(paddingModifier)
                .then(containerModifier)
                .background(containerColor, CircleShape),
        ) {
            Icon(
                imageVector = when (appearance) {
                    MapAppearance.SYSTEM -> Icons.Outlined.SettingsBrightness
                    MapAppearance.LIGHT -> Icons.Outlined.LightMode
                    MapAppearance.DARK -> Icons.Outlined.DarkMode
                },
                contentDescription = stringResource(R.string.map_appearance_action),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MapAppearance.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (option) {
                                MapAppearance.SYSTEM -> stringResource(R.string.map_appearance_system)
                                MapAppearance.LIGHT -> stringResource(R.string.map_appearance_light)
                                MapAppearance.DARK -> stringResource(R.string.map_appearance_dark)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (option) {
                                MapAppearance.SYSTEM -> Icons.Outlined.SettingsBrightness
                                MapAppearance.LIGHT -> Icons.Outlined.LightMode
                                MapAppearance.DARK -> Icons.Outlined.DarkMode
                            },
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onAppearanceChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
