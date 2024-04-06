package com.dot.gallery.feature_node.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.presentation.common.MediaViewModel
import com.dot.gallery.feature_node.presentation.util.Screen


@Composable
fun SearchScreen(
    navigate: (route: String) -> Unit,
    mediaViewModel: MediaViewModel,
    toggleNavbar: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    isScrolling: MutableState<Boolean>,
    searchBarActive: MutableState<Boolean>
) {
    Scaffold(
        topBar = {
            MainSearchBar(
                mediaViewModel = mediaViewModel,
                bottomPadding = paddingValues.calculateBottomPadding(),
                navigate = navigate,
                toggleNavbar = toggleNavbar,
                isScrolling = isScrolling,
                activeState = searchBarActive
            ) {
                var expandedDropdown by remember { mutableStateOf(false) }
                IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.drop_down_cd)
                    )
                }
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.settings_title)) },
                        onClick = {
                            expandedDropdown = false
                            navigate(Screen.SettingsScreen.route)
                        }
                    )
                }
            }
        }
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = it.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp + 64.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            /*item(
                key = "SearchBar"
            ) {
                MainSearchBar(
                    mediaViewModel = mediaViewModel,
                    bottomPadding = paddingValues.calculateBottomPadding(),
                    navigate = navigate,
                    toggleNavbar = toggleNavbar,
                    isScrolling = isScrolling,
                    activeState = searchBarActive
                ) {
                    var expandedDropdown by remember { mutableStateOf(false) }
                    IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.drop_down_cd)
                        )
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.settings_title)) },
                            onClick = {
                                expandedDropdown = false
                                navigate(Screen.SettingsScreen.route)
                            }
                        )
                    }
                }
            }
             */
            item(
                key = "SearchScreenItem"
            ) {
                Text(
                    text = "Search Screen",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }

}