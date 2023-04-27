package com.dot.gallery.feature_node.presentation.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.SettingsType
import com.dot.gallery.ui.theme.GalleryTheme

@Composable
fun SettingsItem(
    item: SettingsEntity
) {
    var checked by remember {
        mutableStateOf(item.isChecked ?: false)
    }
    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = item.icon!!,
            contentDescription = null
        )
    }
    val summary: @Composable () -> Unit = {
        Text(text = item.summary!!)
    }
    val switch: @Composable () -> Unit = {
        Switch(checked = checked, onCheckedChange = null)
    }
    val headlineColor =
        if (item.isHeader)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurface

    val shape = remember {
        when (item.screenPosition) {
            Position.Alone -> RoundedCornerShape(24.dp)
            Position.Bottom -> RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 24.dp,
                bottomEnd = 24.dp
            )

            Position.Middle -> RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )

            Position.Top -> RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )
        }
    }
    val paddingModifier = if (!item.isHeader)
        when (item.screenPosition) {
            Position.Alone -> Modifier.padding(vertical = 16.dp)
            Position.Bottom -> Modifier.padding(top = 1.dp)
            Position.Middle -> Modifier.padding(vertical = 1.dp)
            Position.Top -> Modifier.padding(bottom = 1.dp)
        }
    else Modifier
    val heightModifier = if (item.isHeader) {
        Modifier.height(36.dp)
    } else Modifier
    val backgroundModifier =
        if (!item.isHeader)
            Modifier
                .clip(shape)
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
        else Modifier.background(Color.Transparent)
    ListItem(
        headlineContent = {
            Text(text = item.title)
        },
        supportingContent = if (!item.summary.isNullOrEmpty()) summary else null,
        trailingContent = if (item.type == SettingsType.Switch) switch else null,
        leadingContent = if (item.icon != null) icon else null,
        modifier = Modifier
            .then(paddingModifier)
            .padding(horizontal = 16.dp)
            .then(backgroundModifier)
            .clickable(
                onClick = {
                    if (item.type == SettingsType.Switch) {
                        item.onCheck?.let {
                            checked = !checked
                            it(checked)
                        }
                    } else item.onClick
                }
            )
            .padding(8.dp)
            .then(heightModifier)
            .fillMaxWidth(),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = headlineColor
        )
    )
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun SettingsItemGroupPreview() =
    GalleryTheme {
        Column(
            modifier = Modifier.wrapContentHeight()
        ) {
            SettingsItem(
                item = SettingsEntity(
                    title = "Preview Alone Title",
                    summary = "Preview Summary"
                )
            )
            SettingsItem(
                item = SettingsEntity(
                    title = "Preview Header Title",
                    type = SettingsType.Header
                )
            )
            SettingsItem(
                item = SettingsEntity(
                    title = "Preview Top Title",
                    summary = "Preview Summary",
                    screenPosition = Position.Top
                )
            )
            SettingsItem(
                item = SettingsEntity(
                    title = "Preview Middle Title",
                    summary = "Preview Summary",
                    type = SettingsType.Switch,
                    screenPosition = Position.Middle
                )
            )
            SettingsItem(
                item = SettingsEntity(
                    title = "Preview Bottom Title",
                    summary = "Preview Summary",
                    screenPosition = Position.Bottom
                )
            )
        }
    }