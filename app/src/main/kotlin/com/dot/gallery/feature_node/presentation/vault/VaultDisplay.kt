package com.dot.gallery.feature_node.presentation.vault

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.cellsList
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.LoadingMedia
import com.dot.gallery.feature_node.presentation.picker.PickerActivityContract
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.vault.components.DeleteVaultSheet
import com.dot.gallery.feature_node.presentation.vault.components.EncryptedMediaGridView
import com.dot.gallery.feature_node.presentation.vault.components.NewVaultSheet
import com.dot.gallery.feature_node.presentation.vault.components.SelectVaultSheet
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDisplay(
    navigateUp: () -> Unit,
    navigate: (route: String) -> Unit,
    onCreateVaultClick: () -> Unit,
    vm: VaultViewModel
) {
    val state by vm.mediaState.collectAsStateWithLifecycle()
    val vaults by vm.vaults.collectAsStateWithLifecycle()
    val currentVault by vm.currentVault.collectAsStateWithLifecycle()

    LaunchedEffect(vaults) {
        if (vaults.isNotEmpty()) {
            vm.setVault(currentVault) {
                //TODO: Add error handling
            }
        }
    }
    
    var lastCellIndex by rememberGridSize()

    val pinchState = rememberPinchZoomGridState(
        cellsList = cellsList,
        initialCellsIndex = lastCellIndex
    )

    var canScroll by rememberSaveable { mutableStateOf(true) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        state = rememberTopAppBarState(),
        canScroll = { canScroll },
    )

    LaunchedEffect(pinchState.isZooming) {
        canScroll = !pinchState.isZooming
        lastCellIndex = cellsList.indexOf(pinchState.currentCells)
    }

    val pickerLauncher = rememberLauncherForActivityResult(PickerActivityContract()) { mediaList ->
        mediaList.forEach {
            vm.addMedia(it)
        }
    }

    val scope = rememberCoroutineScope()
    val newVaultSheetState = rememberAppBottomSheetState()
    val deleteVaultSheetState = rememberAppBottomSheetState()
    
    Box {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        pickerLauncher.launch(Unit)
                    }
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = stringResource(R.string.add_media_to_vault_cd))
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            val sheetState = rememberAppBottomSheetState()
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        scope.launch {
                                            if (vaults.size > 1) {
                                                sheetState.show()
                                            }
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = currentVault?.name ?: stringResource(R.string.unknown_vault)
                                )
                                if (vaults.size > 1) {
                                    Icon(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = CircleShape
                                            ),
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            SelectVaultSheet(
                                state = sheetState,
                                vaults = vaults.filterNot { it == currentVault },
                                onVaultSelected = { vault ->
                                    scope.launch {
                                        vm.setVault(vault) {}
                                    }
                                }
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = navigateUp) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back_cd)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) {
            PinchZoomGridLayout(state = pinchState) {
                AnimatedVisibility(
                    visible = state.isLoading,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    LoadingMedia(
                        paddingValues = PaddingValues(
                            top = it.calculateTopPadding() + 56.dp,
                            bottom = it.calculateBottomPadding()
                        )
                    )
                }
                EncryptedMediaGridView(
                    mediaState = state,
                    allowSelection = true,
                    showSearchBar = false,
                    enableStickyHeaders = false,
                    paddingValues = it,
                    canScroll = canScroll,
                    showMonthlyHeader = false,
                    isScrolling = remember { mutableStateOf(false) },
                    aboveGridContent = {
                        Column {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 12.dp)
                                    .horizontalScroll(state = rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = {
                                        scope.launch {
                                            newVaultSheetState.show()
                                        }
                                    },
                                    label = { Text(text = stringResource(R.string.new_vault)) },
                                    border = null,
                                    shape = CircleShape,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = null
                                        )
                                    }
                                )
                                SuggestionChip(
                                    onClick = {
                                        scope.launch {
                                            deleteVaultSheetState.show()
                                        }
                                    },
                                    label = { Text(text = stringResource(R.string.delete_vault)) },
                                    border = null,
                                    shape = CircleShape,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                            val showError = remember(state) { state.error.isNotEmpty() }
                            AnimatedVisibility(
                                visible = showError,
                                modifier = Modifier
                                    .padding(it)
                                    .fillMaxWidth()
                            ) {
                                Error(errorMessage = state.error)
                            }
                            val showEmpty =
                                remember(state) { state.media.isEmpty() && !state.isLoading && !showError }
                            AnimatedVisibility(
                                visible = showEmpty,
                                modifier = Modifier
                                    .padding(it)
                                    .fillMaxWidth()
                            ) {
                                EmptyMedia()
                            }
                        }
                        NewVaultSheet(
                            state = newVaultSheetState,
                            onConfirm = onCreateVaultClick
                        )
                        DeleteVaultSheet(
                            state = deleteVaultSheetState,
                            onConfirm = {
                                vm.deleteVault(currentVault!!)
                                if (vaults.isEmpty()) {
                                    navigateUp()
                                }
                            }
                        )
                    },
                    onMediaClick = { encryptedMedia ->
                        navigate(Screen.EncryptedMediaViewScreen.id(encryptedMedia.id))
                    }
                )
            }
        }
        /*SelectionSheet(
            modifier = Modifier
                .align(Alignment.BottomEnd),
            target = target,
            selectedMedia = selectedMedia,
            selectionState = selectionState,
            albumsState = albumsState,
            handler = handler
        )*/

    }
}