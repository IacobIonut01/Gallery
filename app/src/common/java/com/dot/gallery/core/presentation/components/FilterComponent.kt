/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Settings.Album.rememberLastSort
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType

@Composable
fun FilterButton(
    modifier: Modifier = Modifier,
    filterOptions: Array<FilterOption> = emptyArray()
) {
    var lastSort by rememberLastSort()
    var expanded by remember { mutableStateOf(false) }
    var selectedFilter by remember(lastSort) { mutableStateOf(filterOptions.first { it.selected }.titleRes) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopEnd)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        TextButton(onClick = { expanded = true }) {
            Row {
                Text(
                    modifier = Modifier.padding(end = 4.dp),
                    text = stringResource(selectedFilter)
                )
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = stringResource(R.string.filter)
                )
            }
        }

        DropdownMenu(
            modifier = Modifier
                .align(Alignment.TopEnd),
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (filter in filterOptions) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(filter.titleRes)) },
                    onClick = {
                        filterOptions.forEach {
                            it.selected = it.titleRes == filter.titleRes
                            if (it.selected) selectedFilter = it.titleRes
                        }
                        filter.onClick(filter.mediaOrder)
                        lastSort = filterOptions.indexOf(filter)
                    },
                    trailingIcon = {
                        if (filter.selected)
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null
                            )
                    }
                )
            }
        }
    }
}


data class FilterOption(
    val titleRes: Int = -1,
    val onClick: (MediaOrder) -> Unit = {},
    val mediaOrder: MediaOrder = MediaOrder.Date(OrderType.Descending),
    var selected: Boolean = false
)