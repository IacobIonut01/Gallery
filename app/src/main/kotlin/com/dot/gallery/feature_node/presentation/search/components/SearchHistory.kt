package com.dot.gallery.feature_node.presentation.search.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Settings.Search.rememberSearchHistory

@Composable
fun SearchHistory(search: (query: String) -> Unit) {
    var historySet by rememberSearchHistory()
    val historyItems = remember(historySet) {
        historySet.toList().mapIndexed { index, item ->
            Pair(
                item.substringBefore(
                    delimiter = "/",
                    missingDelimiterValue = index.toString()
                ),
                item.substringAfter(
                    delimiter = "/",
                    missingDelimiterValue = item
                )
            )
        }.sortedByDescending { it.first }
    }
    val suggestionSet = listOf(
        "0" to "Screenshots",
        "1" to "Camera",
        "2" to "May 2022",
        "3" to "Thursday"
    )
    val maxItems = remember(historySet) {
        if (historyItems.size >= 5) 5 else historyItems.size
    }

    LazyColumn {
        if (historyItems.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.history_recent_title),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(top = 16.dp)
                )
            }
            items(historyItems.subList(0, maxItems)) {
                HistoryItem(
                    historyQuery = it,
                    search = search,
                ) {
                    historySet = historySet.toMutableSet().apply { remove(it) }
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.history_suggestions_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(top = 16.dp)
            )
        }
        items(suggestionSet) {
            HistoryItem(
                historyQuery = it,
                search = search,
            )
        }
    }
}