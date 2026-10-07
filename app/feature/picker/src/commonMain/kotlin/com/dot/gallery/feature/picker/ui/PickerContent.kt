/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.picker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.core.image.thumbnail.LocalThumbnailMotion
import com.dot.gallery.core.image.thumbnail.rememberThumbnailMotionState
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.core.presentation.util.selectedMedia
import com.dot.gallery.core.ui.settings.rememberPreference
import com.dot.gallery.core.ui.util.AppBottomSheetState
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.core.ui.util.rememberPlatformToast
import com.dot.gallery.core.ui.util.rememberedDerivedState
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import com.dot.gallery.domain.model.Vault
import com.dot.gallery.domain.settings.SettingsKeys
import com.dot.gallery.domain.util.getUri
import com.dot.gallery.ui.theme.Dimens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.picker.generated.resources.Res
import com.dot.gallery.feature.picker.generated.resources.action_add
import com.dot.gallery.feature.picker.generated.resources.action_add_count
import com.dot.gallery.feature.picker.generated.resources.action_ok
import com.dot.gallery.feature.picker.generated.resources.add_media
import com.dot.gallery.feature.picker.generated.resources.albums
import com.dot.gallery.feature.picker.generated.resources.close
import com.dot.gallery.feature.picker.generated.resources.cloud
import com.dot.gallery.feature.picker.generated.resources.locked
import com.dot.gallery.feature.picker.generated.resources.locked_album_security_error
import com.dot.gallery.feature.picker.generated.resources.navigate_up
import com.dot.gallery.feature.picker.generated.resources.picker_prepare_failed
import com.dot.gallery.feature.picker.generated.resources.picker_preparing
import com.dot.gallery.feature.picker.generated.resources.picker_preparing_generic
import com.dot.gallery.feature.picker.generated.resources.preview
import com.dot.gallery.feature.picker.generated.resources.security_private_folder
import com.dot.gallery.feature.picker.generated.resources.timeline
import com.dot.gallery.feature.picker.generated.resources.vault


private sealed class PickerNavState {
    data class Tabs(val tabIndex: Int) : PickerNavState()
    data class AlbumDetail(val album: Album) : PickerNavState()
    data object PrivateFolder : PickerNavState()
    data object VaultChooser : PickerNavState()
    data class VaultDetail(val vault: Vault) : PickerNavState()
    data object CloudMedia : PickerNavState()
}

/**
 * The whole media-picker body — tab bar, timeline/album/private-folder/cloud/
 * vault drill-ins, selection FAB and the preview overlay host. State in via
 * [state], events out via [actions]; the app shell supplies the two platform
 * slots:
 * - [albumImage]: the album tile (`AlbumImage` lives in `:feature:albums`).
 * - [previewContent]: the standalone media viewer (`MediaViewScreenRoute`
 *   app-side) that previews the current selection. It receives the grid's
 *   shared-transition scopes so cells morph into the viewer, plus [onDismiss]
 *   to leave preview mode.
 */
@Suppress("LABEL_NAME_CLASH")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun PickerContent(
    state: PickerState,
    actions: PickerActions,
    albumImage: @Composable (album: Album, onClick: (Album) -> Unit) -> Unit,
    previewContent: @Composable (
        sharedTransitionScope: SharedTransitionScope,
        animatedVisibilityScope: AnimatedVisibilityScope,
        mediaState: State<MediaState<Media>>,
        mediaId: Long,
        onDismiss: () -> Unit,
    ) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val selector = LocalMediaSelector.current
    val selectedMedia = selector.selectedMedia.collectAsStateWithLifecycle()
    val toast = rememberPlatformToast()
    val pickerPrepareFailedText = stringResource(Res.string.picker_prepare_failed)
    val addMediaContentDescription = stringResource(Res.string.add_media)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    val securitySheetState = rememberAppBottomSheetState()

    val onAlbumClickWithLock: (Album) -> Unit = { album ->
        if (album.isLocked) {
            actions.onLockedAlbum(
                album,
                { selectedAlbum = album },
                { scope.launch { securitySheetState.show() } },
            )
        } else {
            selectedAlbum = album
        }
    }

    PickerSecurityInfoSheet(sheetState = securitySheetState)

    var showPrivateFolder by remember { mutableStateOf(false) }
    var showCloud by remember { mutableStateOf(false) }
    var showVaultChooser by remember { mutableStateOf(false) }
    var selectedPickerVault by remember { mutableStateOf<Vault?>(null) }

    // Progress of decrypting/downloading selected vault/cloud items on "Add"
    var materializing by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val privateFolderUri by rememberPreference(SettingsKeys.privateFolderUri)

    // After the app-side gate is cleared, reveal the vaults: a single vault
    // authenticates directly, multiple show the chooser.
    fun revealVaults() {
        val vaults = state.vaultState.value.vaults
        when {
            vaults.isEmpty() -> {}
            vaults.size == 1 -> {
                val vault = vaults.first()
                actions.onVault(vault) {
                    showVaultChooser = false
                    selectedPickerVault = vault
                }
            }
            else -> showVaultChooser = true
        }
    }

    val navState: PickerNavState = when {
        selectedPickerVault != null -> PickerNavState.VaultDetail(selectedPickerVault!!)
        showVaultChooser -> PickerNavState.VaultChooser
        showCloud -> PickerNavState.CloudMedia
        showPrivateFolder -> PickerNavState.PrivateFolder
        selectedAlbum != null -> PickerNavState.AlbumDetail(selectedAlbum!!)
        else -> PickerNavState.Tabs(selectedTabIndex)
    }

    // Resolving the flow also points the app-side ViewModel's lazy at this album —
    // the original fed it through `mediaVM.albumId` from a LaunchedEffect.
    val albumId = selectedAlbum?.id ?: -1L
    val mediaFlow = remember(albumId) { state.mediaStateFor(albumId) }

    // Observable per-vault media flow, rebuilt (and re-subscribed) whenever the open vault changes.
    val vaultMediaFlow = remember(selectedPickerVault) {
        state.vaultMediaStateFor(selectedPickerVault)
    }
    val currentVaultMediaState by vaultMediaFlow.collectAsStateWithLifecycle()

    // The flow above only holds the currently-open vault. Selection is shared across all sources,
    // so accumulate media from every vault visited this session (keyed by its unique encrypted-file
    // URI) — otherwise picks from a previously-opened vault would be dropped on Add. Cross-vault
    // decryption is safe: KeychainHolder resolves the vault + key from each file's own path.
    val visitedVaultMedia = remember { mutableStateMapOf<String, Media>() }
    LaunchedEffect(currentVaultMediaState.media) {
        currentVaultMediaState.media.forEach { visitedVaultMedia[it.getUri().toString()] = it }
    }
    val vaultMediaForSelection = visitedVaultMedia.values.toList()

    SharedTransitionLayout {
        AnimatedContent(
            targetState = showPreview,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 3 })
                    .togetherWith(fadeOut() + slideOutVertically { -it / 3 })
            },
            label = "previewTransition"
        ) { isPreview ->
            val previewScope = this@AnimatedContent
            if (!isPreview) {
                Scaffold(
                    topBar = {
                        AnimatedContent(
                            targetState = navState,
                            transitionSpec = {
                                (slideInVertically { -it } + fadeIn())
                                    .togetherWith(slideOutVertically { -it } + fadeOut())
                                    .using(SizeTransform(clip = false))
                            },
                            label = "topBarAnimation"
                        ) { currentNavState ->
                            if (currentNavState !is PickerNavState.Tabs) {
                                val detailTitle = when (currentNavState) {
                                    is PickerNavState.AlbumDetail -> currentNavState.album.label
                                    is PickerNavState.PrivateFolder -> stringResource(Res.string.security_private_folder)
                                    is PickerNavState.CloudMedia -> stringResource(Res.string.cloud)
                                    is PickerNavState.VaultChooser -> stringResource(Res.string.vault)
                                    is PickerNavState.VaultDetail -> currentNavState.vault.name
                                    is PickerNavState.Tabs -> ""
                                }
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = detailTitle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            when (currentNavState) {
                                                is PickerNavState.PrivateFolder -> showPrivateFolder = false
                                                is PickerNavState.CloudMedia -> showCloud = false
                                                is PickerNavState.VaultChooser -> showVaultChooser = false
                                                is PickerNavState.VaultDetail -> selectedPickerVault = null
                                                else -> selectedAlbum = null
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(Res.string.navigate_up)
                                            )
                                        }
                                    },
                                    actions = {
                                        if (selectedMedia.value.isNotEmpty()) {
                                            TextButton(onClick = { showPreview = true }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Visibility,
                                                    contentDescription = null
                                                )
                                                Text(
                                                    text = stringResource(Res.string.preview),
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            } else {
                                Column {
                                    TopAppBar(
                                        title = { Text(text = state.title) },
                                        navigationIcon = {
                                            IconButton(onClick = actions.onClose) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = stringResource(Res.string.close)
                                                )
                                            }
                                        },
                                        actions = {
                                            if (selectedMedia.value.isNotEmpty()) {
                                                TextButton(onClick = { showPreview = true }) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Visibility,
                                                        contentDescription = null
                                                    )
                                                    Text(
                                                        text = stringResource(Res.string.preview),
                                                        modifier = Modifier.padding(start = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 8.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            PillTab(
                                                selected = selectedTabIndex == 0,
                                                text = stringResource(Res.string.timeline),
                                                onClick = { selectedTabIndex = 0 }
                                            )
                                            PillTab(
                                                selected = selectedTabIndex == 1,
                                                text = stringResource(Res.string.albums),
                                                onClick = { selectedTabIndex = 1 }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding())
                    ) {
                        AnimatedContent(
                            targetState = navState,
                            transitionSpec = {
                                when (targetState) {
                                    is PickerNavState.AlbumDetail if initialState is PickerNavState.Tabs -> {
                                        (slideInHorizontally { it } + fadeIn())
                                            .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
                                    }

                                    is PickerNavState.Tabs if initialState is PickerNavState.AlbumDetail -> {
                                        (slideInHorizontally { -it / 3 } + fadeIn())
                                            .togetherWith(slideOutHorizontally { it } + fadeOut())
                                    }

                                    is PickerNavState.Tabs if initialState is PickerNavState.Tabs -> {
                                        val targetTab = (targetState as PickerNavState.Tabs).tabIndex
                                        val initialTab = (initialState as PickerNavState.Tabs).tabIndex
                                        val direction = if (targetTab > initialTab) 1 else -1
                                        (slideInHorizontally { direction * it } + fadeIn())
                                            .togetherWith(slideOutHorizontally { -direction * it } + fadeOut())
                                    }

                                    else -> fadeIn() togetherWith fadeOut()
                                }.using(SizeTransform(clip = false))
                            },
                            label = "contentAnimation"
                        ) { nav ->
                            val contentScope = this@AnimatedContent
                            when (nav) {
                                is PickerNavState.AlbumDetail -> {
                                    PickerMediaContent(
                                        mediaState = mediaFlow,
                                        metadataState = state.metadataState,
                                        allowSelection = state.allowSelection,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = previewScope,
                                    )
                                }
                                is PickerNavState.PrivateFolder -> {
                                    PickerMediaContent<Media>(
                                        mediaState = state.privateFolderMediaState,
                                        metadataState = state.metadataState,
                                        allowSelection = state.allowSelection,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = previewScope,
                                    )
                                }
                                is PickerNavState.CloudMedia -> {
                                    PickerMediaContent<Media>(
                                        mediaState = state.cloudMediaState,
                                        metadataState = state.metadataState,
                                        allowSelection = state.allowSelection,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = previewScope,
                                    )
                                }
                                is PickerNavState.VaultDetail -> {
                                    PickerMediaContent<Media>(
                                        mediaState = vaultMediaFlow,
                                        metadataState = state.metadataState,
                                        allowSelection = state.allowSelection,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = previewScope,
                                    )
                                }
                                is PickerNavState.VaultChooser -> {
                                    PickerVaultChooserGrid(
                                        vaults = state.vaultState.value.vaults,
                                        onVaultClick = { vault ->
                                            actions.onVault(vault) {
                                                showVaultChooser = false
                                                selectedPickerVault = vault
                                            }
                                        }
                                    )
                                }
                                is PickerNavState.Tabs -> {
                                    when (nav.tabIndex) {
                                        0 -> {
                                            PickerMediaContent(
                                                mediaState = mediaFlow,
                                                metadataState = state.metadataState,
                                                allowSelection = state.allowSelection,
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = previewScope,
                                            )
                                        }
                                        1 -> {
                                            PickerAlbumsGrid(
                                                albums = state.albumsState.value.albums.filter { it.id != -1L },
                                                onAlbumClick = onAlbumClickWithLock,
                                                showPrivateFolder = privateFolderUri.isNotEmpty(),
                                                onPrivateFolderClick = {
                                                    actions.onPrivateFolder { showPrivateFolder = true }
                                                },
                                                showCloud = state.cloudAvailable,
                                                onCloudClick = { showCloud = true },
                                                showVault = state.vaultState.value.vaults.isNotEmpty(),
                                                onVaultClick = {
                                                    actions.onVaultGate { revealVaults() }
                                                },
                                                albumImage = albumImage,
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedContentScope = contentScope
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(32.dp),
                            visible = state.allowSelection || selectedMedia.value.isNotEmpty(),
                            enter = slideInVertically { it * 2 },
                            exit = slideOutVertically { it * 2 }
                        ) {
                            val enabled by rememberedDerivedState {
                                selectedMedia.value.isNotEmpty()
                            }
                            val containerColor by animateColorAsState(
                                targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                label = "containerColor"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "contentColor"
                            )
                            val mediaState by mediaFlow.collectAsStateWithLifecycle()
                            val privateFolderState by state.privateFolderMediaState.collectAsStateWithLifecycle()
                            val cloudState by state.cloudMediaState.collectAsStateWithLifecycle()
                            // Selection is shared across sources (MediaSelector). Resolve ids against
                            // the union of every loaded source (including all vaults visited this
                            // session) so cross-source picks are honored.
                            val allSourceMedia = remember(
                                mediaState.media, privateFolderState.media,
                                cloudState.media, vaultMediaForSelection
                            ) {
                                (mediaState.media + privateFolderState.media +
                                        cloudState.media + vaultMediaForSelection)
                                    .distinctBy { it.id }
                            }
                            val selectedMediaList = allSourceMedia.selectedMedia(selectedMedia)
                            ExtendedFloatingActionButton(
                                text = {
                                    if (state.allowSelection)
                                        Text(
                                            text = stringResource(
                                                Res.string.action_add_count,
                                                selectedMedia.value.size,
                                            )
                                        )
                                    else
                                        Text(text = stringResource(Res.string.action_add))
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null
                                    )
                                },
                                containerColor = containerColor,
                                contentColor = contentColor,
                                expanded = true,
                                onClick = {
                                    if (enabled && materializing == null) {
                                        scope.launch {
                                            materializing = 0 to selectedMediaList.size
                                            val delivered = try {
                                                actions.onPickConfirmed(selectedMediaList) { done, total ->
                                                    materializing = done to total
                                                }
                                            } finally {
                                                materializing = null
                                            }
                                            if (!delivered) {
                                                toast(pickerPrepareFailedText)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .semantics {
                                        contentDescription = addMediaContentDescription
                                    }
                            )
                            BackHandler(selectedMedia.value.isNotEmpty()) {
                                selector.clearSelection()
                            }
                        }
                    }
                }
            } else {
                val collectedMediaState by mediaFlow.collectAsStateWithLifecycle()
                val collectedPrivateFolderState by state.privateFolderMediaState.collectAsStateWithLifecycle()
                val collectedCloudState by state.cloudMediaState.collectAsStateWithLifecycle()
                val sourceMedia = (collectedMediaState.media + collectedPrivateFolderState.media +
                        collectedCloudState.media + vaultMediaForSelection).distinctBy { it.id }
                val previewMediaList = sourceMedia.selectedMedia(selectedMedia)
                if (previewMediaList.isNotEmpty()) {
                    val previewMediaState = remember(previewMediaList) {
                        mutableStateOf(
                            MediaState(
                                media = previewMediaList,
                                isLoading = false
                            )
                        )
                    }
                    previewContent(
                        this@SharedTransitionLayout,
                        this@AnimatedContent,
                        previewMediaState,
                        previewMediaList.first().id,
                    ) { showPreview = false }
                } else {
                    LaunchedEffect(Unit) { showPreview = false }
                }
            }
        }
    }

    materializing?.let { (done, total) ->
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = if (total > 0) {
                            stringResource(Res.string.picker_preparing, done, total)
                        } else stringResource(Res.string.picker_preparing_generic),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    BackHandler(selectedPickerVault != null) {
        selectedPickerVault = null
    }

    BackHandler(showVaultChooser) {
        showVaultChooser = false
    }

    BackHandler(showCloud) {
        showCloud = false
    }

    BackHandler(showPrivateFolder) {
        showPrivateFolder = false
    }

    BackHandler(selectedAlbum != null) {
        selectedAlbum = null
    }
}

@Composable
private fun RowScope.PillTab(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "pillTabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pillTabText"
    )
    Surface(
        modifier = Modifier
            .weight(1f)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSecurityInfoSheet(
    sheetState: AppBottomSheetState
) {
    val scope = rememberCoroutineScope()
    if (sheetState.isVisible) {
        ModalBottomSheet(
            sheetState = sheetState.sheetState,
            onDismissRequest = {
                scope.launch { sheetState.hide() }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.locked),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    text = stringResource(Res.string.locked_album_security_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                SetupButton(
                    onClick = {
                        scope.launch { sheetState.hide() }
                    },
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    text = stringResource(Res.string.action_ok)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PickerAlbumsGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    showPrivateFolder: Boolean = false,
    onPrivateFolderClick: () -> Unit = {},
    showCloud: Boolean = false,
    onCloudClick: () -> Unit = {},
    showVault: Boolean = false,
    onVaultClick: () -> Unit = {},
    albumImage: @Composable (album: Album, onClick: (Album) -> Unit) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    // Phase 3 (#1076): album covers (RenderImage) load the cheap MOTION tier while scrolling.
    val gridState = rememberLazyGridState()
    val thumbnailMotion = rememberThumbnailMotionState({ gridState.isScrollInProgress })
    CompositionLocalProvider(LocalThumbnailMotion provides thumbnailMotion) {
    with(sharedTransitionScope) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(Dimens.Album()),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = albums,
                key = { it.toString() }
            ) { album ->
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "album_${album.id}"),
                                animatedVisibilityScope = animatedContentScope,
                                enter = fadeIn(),
                                exit = fadeOut()
                            )
                    ) {
                        albumImage(album, onAlbumClick)
                    }
                    Text(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .padding(horizontal = 16.dp),
                        text = album.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
            if (showPrivateFolder) {
                item(key = "private_folder") {
                    PickerSourceCard(
                        icon = Icons.Outlined.FolderOff,
                        label = stringResource(Res.string.security_private_folder),
                        onClick = onPrivateFolderClick
                    )
                }
            }
            if (showCloud) {
                item(key = "cloud_source") {
                    PickerSourceCard(
                        icon = Icons.Outlined.Cloud,
                        label = stringResource(Res.string.cloud),
                        onClick = onCloudClick
                    )
                }
            }
            if (showVault) {
                item(key = "vault_source") {
                    PickerSourceCard(
                        icon = Icons.Outlined.Lock,
                        label = stringResource(Res.string.vault),
                        onClick = onVaultClick
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun PickerSourceCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp),
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

@Composable
private fun PickerVaultChooserGrid(
    vaults: List<Vault>,
    onVaultClick: (Vault) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(Dimens.Album()),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = vaults,
            key = { it.uuid.toString() }
        ) { vault ->
            PickerSourceCard(
                icon = Icons.Outlined.Lock,
                label = vault.name,
                onClick = { onVaultClick(vault) }
            )
        }
    }
}
