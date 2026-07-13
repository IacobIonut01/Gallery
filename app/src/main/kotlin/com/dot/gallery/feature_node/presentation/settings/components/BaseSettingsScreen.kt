package com.dot.gallery.feature_node.presentation.settings.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.feature_node.presentation.help.data.SettingsSearchRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseSettingsScreen(
    title: String,
    settingsList: SnapshotStateList<SettingsEntity>,
    settingsBuilder: @Composable (SettingsEntity, Int) -> Unit = { it, index ->
        SettingsItem(it)
    },
    topContent: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    /**
     * When set, the non-header toggle titles rendered here are registered into
     * [SettingsSearchRegistry] under this screen route so Help & Tips search can
     * find individual settings without a hand-maintained catalog.
     */
    searchRoute: String? = null
) {
    if (searchRoute != null) {
        val toggleTitles = settingsList
            .filter { !it.isHeader && it.title.isNotBlank() }
            .map { it.title }
        LaunchedEffect(searchRoute, toggleTitles) {
            SettingsSearchRegistry.register(searchRoute, toggleTitles)
        }
    }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = { NavigationBackButton() },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                end = padding.calculateEndPadding(LocalLayoutDirection.current),
                top = 16.dp + padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding()
            ),
        ) {
            if (topContent != null) {
                item {
                    topContent()
                }
            }
            itemsIndexed(
                items = settingsList,
                key = { index, it -> "$index-${it.title}-${it.type}" }
            ) { index, it ->
                settingsBuilder(it, index)
            }
            if (bottomContent != null) {
                item {
                    bottomContent()
                }
            }
        }
    }
}