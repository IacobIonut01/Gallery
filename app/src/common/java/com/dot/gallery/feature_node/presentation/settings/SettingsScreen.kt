/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dot.gallery.R
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.SettingsType.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigateUp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var expandedDropDown by remember { mutableStateOf(false) }

    val settingsList = remember { mutableStateListOf<SettingsEntity>().apply { this.addAll(viewModel.settingsList(context)) } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { expandedDropDown = !expandedDropDown }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.drop_down_cd)
                        )
                    }
                    DropdownMenu(
                        modifier = Modifier,
                        expanded = expandedDropDown,
                        onDismissRequest = { expandedDropDown = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.reset_settings)
                                )
                            },
                            onClick = {
                                viewModel.settings.resetToDefaults()
                                expandedDropDown = false
                                navigateUp()
                            },
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(
                items = settingsList,
                key = { it.title + it.type.toString() }
            ) { item ->
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
                if (item.type == Header) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .padding(top = 8.dp),
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    ListItem(
                        headlineContent = {
                            Text(text = item.title)
                        },
                        supportingContent = if (!item.summary.isNullOrEmpty()) summary else null,
                        trailingContent = if (item.type == Switch) switch else null,
                        leadingContent = if (item.icon != null) icon else null,
                        modifier = Modifier
                            .clickable(
                                onClick = {
                                    if (item.type == Switch) {
                                        item.onCheck?.let {
                                            checked = !checked
                                            it(checked)
                                        }
                                    } else item.onClick
                                }
                            )
                    )
                }
            }
        }
    }

}