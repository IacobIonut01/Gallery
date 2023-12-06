package com.dot.gallery.feature_node.presentation.ignored

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.SettingsEntity
import com.dot.gallery.feature_node.domain.model.BlacklistedAlbum
import com.dot.gallery.feature_node.presentation.settings.components.SettingsItem
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoredScreen(
    navigateUp: () -> Unit,
) {

    val vm = hiltViewModel<IgnoredViewModel>()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val state by vm.blacklistState.collectAsStateWithLifecycle(IgnoredState())
    val selectAlbumState = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    var toBeRemoved by remember(state) {
        mutableStateOf<BlacklistedAlbum?>(null)
    }
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ignored_albums),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                if (state.albums.isNotEmpty()) {
                    SettingsItem(
                        item = SettingsEntity.Header(stringResource(R.string.ignored_albums))
                    )
                }

                state.albums.forEachIndexed { index, blacklistedAlbum ->
                    val position = remember(state.albums) {
                        if (index == 0) {
                            if (state.albums.size == 1) Position.Alone
                            else Position.Top
                        } else if (index == state.albums.size - 1) Position.Bottom
                        else Position.Middle
                    }
                    SettingsItem(
                        item = SettingsEntity.Preference(
                            title = blacklistedAlbum.label,
                            screenPosition = position,
                            onClick = {
                                toBeRemoved = blacklistedAlbum
                            }
                        )
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    scope.launch {
                        selectAlbumState.show()
                    }
                },
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.BottomEnd),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
            }

        }
    }

    if (toBeRemoved != null) {
        AlertDialog(
            onDismissRequest = { toBeRemoved = null },
            confirmButton = {
                Button(
                    onClick = {
                        vm.removeFromBlacklist(toBeRemoved!!)
                        toBeRemoved = null
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { toBeRemoved = null }
                ) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            },
            title = {
                Text(text = stringResource(R.string.remove_from_ignored))
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.remove_from_ignored_summary,
                        toBeRemoved!!.label
                    )
                )
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }

    SelectAlbumSheet(
        sheetState = selectAlbumState,
        blacklistedAlbums = state.albums,
        onSelect = {
            vm.addToBlacklist(BlacklistedAlbum(it.id, it.label))
        }
    )

}

