package com.dot.gallery.app.presentation.ignored

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.domain.model.AlbumState
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.feature.ignored.ui.IgnoredContent
import com.dot.gallery.feature.ignored.ui.IgnoredViewModel
import com.dot.gallery.feature.ignored.ui.setup.IgnoredOptionsSheet
import com.dot.gallery.feature.ignored.ui.setup.IgnoredOptionsViewModel
import com.dot.gallery.feature.ignored.ui.setup.IgnoredSetupSheet
import com.dot.gallery.feature.ignored.ui.setup.IgnoredSetupViewModel
import kotlinx.coroutines.launch
import com.dot.gallery.feature.ignored.ui.setup.IgnoredOptionsSheet as SharedIgnoredOptionsSheet
import com.dot.gallery.feature.ignored.ui.setup.IgnoredSetupSheet as SharedIgnoredSetupSheet
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.injection.injectViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgnoredScreen(
    albumsState: State<AlbumState>,
) {
    val vm = injectViewModel<IgnoredViewModel> {ignoredViewModel}
    val optionsVm = injectViewModel<IgnoredOptionsViewModel> {ignoredOptionsViewModel}
    val setupVm = injectViewModel<IgnoredSetupViewModel> {ignoredSetupViewModel}
    val state by vm.blacklistState.collectAsStateWithLifecycle()
    val optionsUiState by optionsVm.uiState.collectAsStateWithLifecycle()
    val setupUiState by setupVm.uiState.collectAsStateWithLifecycle()
    val setupSheetState = rememberAppBottomSheetState()
    val optionsSheetState = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()
    var selectedAlbum by remember { mutableStateOf<IgnoredAlbum?>(null) }
    val albumGridSize by rememberAlbumGridSize()

    IgnoredContent(
        state = state,
        albumsState = albumsState,
        onAddClick = {
            scope.launch { setupSheetState.show() }
        },
        onAlbumClick = { album ->
            selectedAlbum = album
            scope.launch { optionsSheetState.show() }
        }
    )

    LaunchedEffect(setupSheetState.isVisible, albumsState.value.albumsWithBlacklisted) {
        if (setupSheetState.isVisible) {
            setupVm.updateAlbums(albumsState.value.albumsWithBlacklisted)
        }
    }

    LaunchedEffect(albumGridSize) {
        optionsVm.updateAlbumGridSize(albumGridSize)
        setupVm.updateAlbumGridSize(albumGridSize)
    }

    LaunchedEffect(optionsSheetState.isVisible, selectedAlbum) {
        if (optionsSheetState.isVisible && selectedAlbum != null) {
            optionsVm.initialize(
                ignoredAlbum = selectedAlbum,
                albums = albumsState.value.albumsWithBlacklisted,
                onUpdate = vm::updateIgnoredAlbum,
                onDelete = vm::removeFromBlacklist
            )
        } else if (!optionsSheetState.isVisible) {
            optionsVm.reset()
        }
    }

    LaunchedEffect(optionsSheetState.isVisible, albumsState.value.albumsWithBlacklisted) {
        if (optionsSheetState.isVisible) {
            optionsVm.updateAlbums(albumsState.value.albumsWithBlacklisted)
        }
    }

    IgnoredSetupSheet(
        sheetState = setupSheetState,
        uiState = setupUiState,
        onAction = setupVm::onAction,
        onDismiss = setupVm::reset,
    )

    IgnoredOptionsSheet(
        sheetState = optionsSheetState,
        uiState = optionsUiState,
        albumState = albumsState,
        onAction = optionsVm::onAction,
        onDismiss = optionsVm::reset,
    )
}
