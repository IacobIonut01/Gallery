package com.dot.gallery.feature_node.presentation.vault

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.cellsList
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.presentation.components.EmptyMedia
import com.dot.gallery.feature_node.domain.model.EncryptedMediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.model.VaultState
import com.dot.gallery.feature_node.presentation.picker.PickerActivityContract
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
    mediaState: State<EncryptedMediaState>,
    vaultState: VaultState,
    currentVault: MutableState<Vault?>,
    onCreateVaultClick: () -> Unit,
    addMediaToVault: (Vault, Media) -> Unit,
    setVault: (Vault) -> Unit,
    deleteVault: (Vault) -> Unit
) {

    LaunchedEffect(vaultState, currentVault.value) {
        currentVault.value?.let { setVault(it) } ?: run {
            vaultState.vaults.firstOrNull()?.let { setVault(it) }
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
            currentVault.value?.let { vault -> addMediaToVault(vault, it) }
        }
    }

    val scope = rememberCoroutineScope()
    val newVaultSheetState = rememberAppBottomSheetState()
    val deleteVaultSheetState = rememberAppBottomSheetState()

    LaunchedEffect(vaultState) {
        if (vaultState.isLoading) return@LaunchedEffect
        if (vaultState.vaults.isNotEmpty()) return@LaunchedEffect
        navigateUp()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pickerLauncher.launch(Unit)
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_media_to_vault_cd)
                )
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
                                .clickable(
                                    enabled = remember(vaultState) {
                                        vaultState.vaults.size > 1
                                    },
                                ) {
                                    scope.launch {
                                        sheetState.show()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = currentVault.value?.name ?: stringResource(R.string.unknown_vault)
                            )
                            androidx.compose.animation.AnimatedVisibility(
                                visible = vaultState.vaults.size > 1,
                                enter = enterAnimation,
                                exit = exitAnimation
                            ) {
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
                            vaultState = vaultState
                        ) { vault ->
                            scope.launch {
                                setVault(vault)
                            }
                        }
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
            EncryptedMediaGridView(
                mediaState = mediaState,
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
                    }
                    NewVaultSheet(
                        state = newVaultSheetState,
                        onConfirm = onCreateVaultClick
                    )
                    DeleteVaultSheet(
                        state = deleteVaultSheetState,
                        onConfirm = {
                            val vault = currentVault.value ?: vaultState.vaults.firstOrNull()
                            vault?.let { it1 -> deleteVault(it1) }
                            if (vaultState.vaults.isEmpty()) {
                                navigateUp()
                            }
                        }
                    )
                },
                onMediaClick = { encryptedMedia ->
                    navigate(VaultScreens.EncryptedMediaViewScreen.id(encryptedMedia.id))
                },
                emptyContent = {
                    EmptyMedia()
                }
            )
        }
    }

}