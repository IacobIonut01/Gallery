package com.dot.gallery.feature_node.presentation.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun HistoryItem(
    historyQuery: Pair<String, String>,
    search: (String) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = historyQuery.second,
                    modifier = Modifier
                        .weight(1f)
                )
                if (onDelete != null) {
                    IconButton(
                        onClick = {
                            val timestamp = if (historyQuery.first.length < 10) "" else "${historyQuery.first}/"
                            onDelete("$timestamp${historyQuery.second}")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null
                        )
                    }
                }
            }
        },
        leadingContent = {
            val icon = if (onDelete != null)
                Icons.Outlined.History
            else Icons.Outlined.Search
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .clickable { search(historyQuery.second) }
    )
}
