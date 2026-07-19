/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.presentation.picker.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import com.dot.gallery.core.presentation.components.SetupButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.LocalMediaSelector
import com.dot.gallery.core.setFollowTheme
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.AlbumState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.model.VaultState
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.albums.components.AlbumImage
import com.dot.gallery.feature_node.presentation.mediaview.MediaViewScreenRoute
import com.dot.gallery.feature_node.presentation.mediaview.rememberedDerivedState
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import com.dot.gallery.feature_node.presentation.picker.PickerViewModel
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.rememberWindowInsetsController
import com.dot.gallery.feature_node.presentation.util.selectedMedia
import com.dot.gallery.feature_node.presentation.util.toggleOrientation
import com.dot.gallery.feature_node.presentation.vault.components.VaultPasswordUnlockDialog
import com.dot.gallery.feature_node.presentation.vault.utils.GateMode
import com.dot.gallery.feature_node.presentation.vault.utils.VaultAuthType
import com.dot.gallery.feature_node.presentation.vault.utils.VaultPasswordManager
import com.dot.gallery.feature_node.presentation.vault.utils.VerifyResult
import com.dot.gallery.feature_node.presentation.vault.utils.rememberBiometricState
import com.dot.gallery.ui.theme.Dimens
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

private sealed class PickerNavState {
    data class Tabs(val tabIndex: Int) : PickerNavState()
    data class AlbumDetail(val album: Album) : PickerNavState()
    data object PrivateFolder : PickerNavState()
    data object VaultChooser : PickerNavState()
    data class VaultDetail(val vault: Vault) : PickerNavState()
    data object CloudMedia : PickerNavState()
}

@Suppress("LABEL_NAME_CLASH")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PickerScreen(
    title: String,
    allowedMedia: AllowedMedia,
    allowSelection: Boolean,
    onClose: () -> Unit,
    sendMediaAsResult: (List<Uri>) -> Unit,
    sendMediaAsMediaResult: (List<Media>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val mediaVM = hiltViewModel<PickerViewModel>().apply {
        this.allowedMedia = allowedMedia
    }
    val albumsState by mediaVM.albumsState.collectAsStateWithLifecycle()
    val metadataState = mediaVM.metadataState.collectAsStateWithLifecycle()
    val selector = LocalMediaSelector.current
    val selectedMedia = selector.selectedMedia.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var pendingLockedAlbum by remember { mutableStateOf<Album?>(null) }
    val securitySheetState = rememberAppBottomSheetState()
    val biometricState = rememberBiometricState(
        title = stringResource(R.string.biometric_authentication),
        subtitle = stringResource(R.string.unlock_album_biometric_subtitle),
        onSuccess = {
            pendingLockedAlbum?.let { album ->
                selectedAlbum = album
            }
            pendingLockedAlbum = null
        },
        onFailed = {
            pendingLockedAlbum = null
        }
    )
    val onAlbumClickWithLock: (Album) -> Unit = remember(biometricState) {
        { album ->
            if (album.isLocked) {
                if (!biometricState.isSupported) {
                    scope.launch { securitySheetState.show() }
                } else {
                    pendingLockedAlbum = album
                    biometricState.authenticate()
                }
            } else {
                selectedAlbum = album
            }
        }
    }

    PickerSecurityInfoSheet(sheetState = securitySheetState)

    // Private folder auth state
    val context = LocalContext.current
    var showPrivateFolder by remember { mutableStateOf(false) }
    var showPrivateFolderPasswordDialog by remember { mutableStateOf(false) }
    var privateFolderAuthType by remember { mutableStateOf<VaultAuthType?>(null) }
    var privateFolderPasswordError by remember { mutableStateOf<String?>(null) }
    val privateFolderUri by Settings.Security.rememberPrivateFolderUri()

    val privateFolderBiometricState = rememberBiometricState(
        title = stringResource(R.string.private_folder_unlock),
        subtitle = stringResource(R.string.private_folder_unlock_subtitle),
        onSuccess = { showPrivateFolder = true },
        onFailed = { /* dismissed */ }
    )

    val wrongPasswordStr = stringResource(R.string.vault_wrong_password_attempts)
    val lockedOutStr = stringResource(R.string.vault_locked_out)

    fun authenticatePrivateFolder() {
        scope.launch {
            when (VaultPasswordManager.getPrivateFolderMode(context)) {
                GateMode.NONE -> showPrivateFolder = true
                GateMode.DEVICE -> {
                    if (privateFolderBiometricState.isSupported) {
                        privateFolderBiometricState.authenticate()
                    } else {
                        showPrivateFolder = true
                    }
                }
                GateMode.CUSTOM -> {
                    val authType = VaultPasswordManager.getAuthType(
                        context, VaultPasswordManager.PRIVATE_FOLDER_UUID
                    )
                    if (authType != null) {
                        privateFolderAuthType = authType
                        showPrivateFolderPasswordDialog = true
                    } else {
                        showPrivateFolder = true
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showPrivateFolderPasswordDialog,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            VaultPasswordUnlockDialog(
                authType = privateFolderAuthType,
                onDismiss = {
                    showPrivateFolderPasswordDialog = false
                    privateFolderPasswordError = null
                },
                onSubmit = { secret ->
                    scope.launch {
                        val result = VaultPasswordManager.verifyPassword(
                            context, VaultPasswordManager.PRIVATE_FOLDER_UUID, secret
                        )
                        when (result) {
                            is VerifyResult.Success -> {
                                showPrivateFolderPasswordDialog = false
                                privateFolderPasswordError = null
                                showPrivateFolder = true
                            }
                            is VerifyResult.Failed -> {
                                privateFolderPasswordError = String.format(
                                    wrongPasswordStr,
                                    result.attemptsLeft
                                )
                            }
                            is VerifyResult.LockedOut -> {
                                val seconds = result.cooldownMs / 1000
                                privateFolderPasswordError = String.format(
                                    lockedOutStr, seconds
                                )
                            }
                        }
                    }
                },
                errorMessage = privateFolderPasswordError
            )
        }
    }

    // Cloud + Vault sources
    val cloudAvailable = remember { mediaVM.cloudAvailable }
    val vaultState by mediaVM.vaultState.collectAsStateWithLifecycle()
    var showCloud by remember { mutableStateOf(false) }
    var showVaultChooser by remember { mutableStateOf(false) }
    var selectedPickerVault by remember { mutableStateOf<Vault?>(null) }

    // Progress of decrypting/downloading selected vault/cloud items on "Add"
    var materializing by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Per-vault auth state (mirrors VaultScreen)
    var pendingAuthVault by remember { mutableStateOf<Vault?>(null) }
    var showVaultPasswordDialog by remember { mutableStateOf(false) }
    var vaultAuthType by remember { mutableStateOf<VaultAuthType?>(null) }
    var vaultPasswordError by remember { mutableStateOf<String?>(null) }

    // Vault-list gate auth state (mirrors VaultScreen gate). Persists for the session so the
    // selector password/biometric isn't re-prompted every time the Vault card is tapped.
    var isGateAuthenticated by rememberSaveable { mutableStateOf(false) }
    var showGatePasswordDialog by remember { mutableStateOf(false) }
    var gateAuthType by remember { mutableStateOf<VaultAuthType?>(null) }
    var gatePasswordError by remember { mutableStateOf<String?>(null) }

    fun onVaultAuthSuccess(vault: Vault) {
        showVaultChooser = false
        selectedPickerVault = vault
    }

    val vaultBiometricState = rememberBiometricState(
        title = stringResource(R.string.biometric_authentication),
        subtitle = stringResource(R.string.verify_identity),
        onSuccess = {
            pendingAuthVault?.let { onVaultAuthSuccess(it) }
            pendingAuthVault = null
        },
        onFailed = { pendingAuthVault = null }
    )

    fun authenticateVault(vault: Vault) {
        scope.launch {
            pendingAuthVault = vault
            val authType = VaultPasswordManager.getAuthType(context, vault.uuid)
            if (authType != null) {
                vaultAuthType = authType
                showVaultPasswordDialog = true
            } else if (vaultBiometricState.isSupported) {
                vaultAuthType = null
                vaultBiometricState.authenticate()
            } else {
                onVaultAuthSuccess(vault)
                pendingAuthVault = null
            }
        }
    }

    // After the gate is cleared, reveal the vaults: single vault authenticates directly,
    // multiple show the chooser.
    fun revealVaults() {
        val vaults = vaultState.vaults
        when {
            vaults.isEmpty() -> {}
            vaults.size == 1 -> authenticateVault(vaults.first())
            else -> showVaultChooser = true
        }
    }

    fun onGateAuthSuccess() {
        isGateAuthenticated = true
        showGatePasswordDialog = false
        gatePasswordError = null
        revealVaults()
    }

    val gateBiometricState = rememberBiometricState(
        title = stringResource(R.string.biometric_authentication),
        subtitle = stringResource(R.string.verify_identity),
        onSuccess = { onGateAuthSuccess() },
        onFailed = { /* dismissed */ }
    )

    fun triggerGateAuth() {
        scope.launch {
            when (VaultPasswordManager.getGateMode(context)) {
                GateMode.NONE -> onGateAuthSuccess()
                GateMode.DEVICE -> {
                    if (gateBiometricState.isSupported) {
                        gateBiometricState.authenticate()
                    } else {
                        onGateAuthSuccess()
                    }
                }
                GateMode.CUSTOM -> {
                    val authType = VaultPasswordManager.getAuthType(
                        context, VaultPasswordManager.GATE_UUID
                    )
                    if (authType != null) {
                        gateAuthType = authType
                        showGatePasswordDialog = true
                    } else {
                        onGateAuthSuccess()
                    }
                }
            }
        }
    }

    fun onVaultCardClick() {
        if (isGateAuthenticated) revealVaults() else triggerGateAuth()
    }

    val navState: PickerNavState = when {
        selectedPickerVault != null -> PickerNavState.VaultDetail(selectedPickerVault!!)
        showVaultChooser -> PickerNavState.VaultChooser
        showCloud -> PickerNavState.CloudMedia
        showPrivateFolder -> PickerNavState.PrivateFolder
        selectedAlbum != null -> PickerNavState.AlbumDetail(selectedAlbum!!)
        else -> PickerNavState.Tabs(selectedTabIndex)
    }

    LaunchedEffect(selectedAlbum) {
        mediaVM.albumId = selectedAlbum?.id ?: -1L
    }

    // Observable per-vault media flow, rebuilt (and re-subscribed) whenever the open vault changes.
    val vaultMediaFlow = remember(selectedPickerVault) {
        mediaVM.createVaultMediaState(selectedPickerVault)
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

    val eventHandler = LocalEventHandler.current
    val activity = LocalActivity.current as? ComponentActivity
    val windowInsetsController = rememberWindowInsetsController()

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
                                    is PickerNavState.PrivateFolder -> stringResource(R.string.security_private_folder)
                                    is PickerNavState.CloudMedia -> stringResource(R.string.cloud)
                                    is PickerNavState.VaultChooser -> stringResource(R.string.vault)
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
                                                contentDescription = stringResource(R.string.navigate_up)
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
                                                    text = stringResource(R.string.preview),
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            } else {
                                Column {
                                    TopAppBar(
                                        title = { Text(text = title) },
                                        navigationIcon = {
                                            IconButton(onClick = onClose) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = stringResource(R.string.close)
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
                                                        text = stringResource(R.string.preview),
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
                                                text = stringResource(R.string.timeline),
                                                onClick = { selectedTabIndex = 0 }
                                            )
                                            PillTab(
                                                selected = selectedTabIndex == 1,
                                                text = stringResource(R.string.albums),
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
                        ) { state ->
                            val contentScope = this@AnimatedContent
                            when (state) {
                                is PickerNavState.AlbumDetail -> {
                                    PickerMediaScreen(
                                        mediaState = mediaVM.mediaState.value,
                                        metadataState = metadataState,
                                        allowSelection = allowSelection,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = previewScope,
                                    )
                                }
                                is PickerNavState.PrivateFolder -> {
                                    PickerMediaScreen<Media>(
                                        mediaState = mediaVM.privateFolderMediaState,
                                        metadataState = metadataState,
                                        allowSelection = allowSelection,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = previewScope,
                                    )
                                }
                                is PickerNavState.CloudMedia -> {
                                    PickerMediaScreen<Media>(
                                        mediaState = mediaVM.cloudMediaState,
                                        metadataState = metadataState,
                                        allowSelection = allowSelection,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = previewScope,
                                    )
                                }
                                is PickerNavState.VaultDetail -> {
                                    PickerMediaScreen<Media>(
                                        mediaState = vaultMediaFlow,
                                        metadataState = metadataState,
                                        allowSelection = allowSelection,
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = previewScope,
                                    )
                                }
                                is PickerNavState.VaultChooser -> {
                                    PickerVaultChooserGrid(
                                        vaults = vaultState.vaults,
                                        onVaultClick = { authenticateVault(it) }
                                    )
                                }
                                is PickerNavState.Tabs -> {
                                    when (state.tabIndex) {
                                        0 -> {
                                            PickerMediaScreen(
                                                mediaState = mediaVM.mediaState.value,
                                                metadataState = metadataState,
                                                allowSelection = allowSelection,
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = previewScope,
                                            )
                                        }
                                        1 -> {
                                            PickerAlbumsGrid(
                                                albums = albumsState.albums.filter { it.id != -1L },
                                                onAlbumClick = onAlbumClickWithLock,
                                                showPrivateFolder = privateFolderUri.isNotEmpty(),
                                                onPrivateFolderClick = { authenticatePrivateFolder() },
                                                showCloud = cloudAvailable,
                                                onCloudClick = { showCloud = true },
                                                showVault = vaultState.vaults.isNotEmpty(),
                                                onVaultClick = { onVaultCardClick() },
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
                            visible = allowSelection || selectedMedia.value.isNotEmpty(),
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
                            val mediaState by mediaVM.mediaState.value.collectAsStateWithLifecycle()
                            val privateFolderState by mediaVM.privateFolderMediaState.collectAsStateWithLifecycle()
                            val cloudState by mediaVM.cloudMediaState.collectAsStateWithLifecycle()
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
                                    if (allowSelection)
                                        Text(text = "Add (${selectedMedia.value.size})")
                                    else
                                        Text(text = "Add")
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
                                            val uris = try {
                                                mediaVM.materializeToShareableUris(selectedMediaList) { done, total ->
                                                    materializing = done to total
                                                }
                                            } finally {
                                                materializing = null
                                            }
                                            if (uris.isEmpty()) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.picker_prepare_failed),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                sendMediaAsResult(uris)
                                                sendMediaAsMediaResult(selectedMediaList)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .semantics {
                                        contentDescription = "Add media"
                                    }
                            )
                            BackHandler(selectedMedia.value.isNotEmpty()) {
                                selector.clearSelection()
                            }
                        }
                    }
                }
            } else {
                val collectedMediaState by mediaVM.mediaState.value.collectAsStateWithLifecycle()
                val collectedPrivateFolderState by mediaVM.privateFolderMediaState.collectAsStateWithLifecycle()
                val collectedCloudState by mediaVM.cloudMediaState.collectAsStateWithLifecycle()
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
                    val emptyAlbumState = remember { mutableStateOf(AlbumState()) }
                    val emptyVaultState = remember { mutableStateOf(VaultState(isLoading = false)) }

                    val previousNavigateUp = remember { eventHandler.navigateUpAction }
                    val previousLightStatusBars = remember { windowInsetsController.isAppearanceLightStatusBars }
                    LaunchedEffect(Unit) {
                        windowInsetsController.isAppearanceLightStatusBars = false
                        eventHandler.setFollowTheme(false)
                        eventHandler.navigateUpAction = {
                            windowInsetsController.isAppearanceLightStatusBars = previousLightStatusBars
                            eventHandler.setFollowTheme(true)
                            showPreview = false
                        }
                    }

                    Scaffold { paddingValues ->
                        MediaViewScreenRoute(
                            toggleRotate = { activity?.toggleOrientation() },
                            paddingValues = paddingValues,
                            isStandalone = true,
                            mediaId = previewMediaList.first().id,
                            mediaState = previewMediaState,
                            metadataState = metadataState,
                            albumsState = emptyAlbumState,
                            vaultState = emptyVaultState,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedContentScope = this@AnimatedContent
                        )
                    }

                    BackHandler {
                        windowInsetsController.isAppearanceLightStatusBars = previousLightStatusBars
                        eventHandler.navigateUpAction = previousNavigateUp
                        eventHandler.setFollowTheme(true)
                        showPreview = false
                    }
                } else {
                    LaunchedEffect(Unit) { showPreview = false }
                }
            }
        }
    }

    if (showVaultPasswordDialog) {
        Dialog(
            onDismissRequest = {
                showVaultPasswordDialog = false
                vaultPasswordError = null
                pendingAuthVault = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                VaultPasswordUnlockDialog(
                    authType = vaultAuthType,
                    onDismiss = {
                        showVaultPasswordDialog = false
                        vaultPasswordError = null
                        pendingAuthVault = null
                    },
                    onSubmit = { secret ->
                        val vault = pendingAuthVault
                        if (vault != null) {
                            scope.launch {
                                when (val result = VaultPasswordManager.verifyPassword(
                                    context, vault.uuid, secret
                                )) {
                                    is VerifyResult.Success -> {
                                        showVaultPasswordDialog = false
                                        vaultPasswordError = null
                                        onVaultAuthSuccess(vault)
                                        pendingAuthVault = null
                                    }
                                    is VerifyResult.Failed -> {
                                        vaultPasswordError = String.format(
                                            wrongPasswordStr, result.attemptsLeft
                                        )
                                    }
                                    is VerifyResult.LockedOut -> {
                                        val seconds = result.cooldownMs / 1000
                                        vaultPasswordError = String.format(lockedOutStr, seconds)
                                    }
                                }
                            }
                        }
                    },
                    errorMessage = vaultPasswordError
                )
            }
        }
    }

    if (showGatePasswordDialog) {
        Dialog(
            onDismissRequest = {
                showGatePasswordDialog = false
                gatePasswordError = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                VaultPasswordUnlockDialog(
                    authType = gateAuthType,
                    onDismiss = {
                        showGatePasswordDialog = false
                        gatePasswordError = null
                    },
                    onSubmit = { secret ->
                        scope.launch {
                            when (val result = VaultPasswordManager.verifyPassword(
                                context, VaultPasswordManager.GATE_UUID, secret
                            )) {
                                is VerifyResult.Success -> onGateAuthSuccess()
                                is VerifyResult.Failed -> {
                                    gatePasswordError = String.format(
                                        wrongPasswordStr, result.attemptsLeft
                                    )
                                }
                                is VerifyResult.LockedOut -> {
                                    val seconds = result.cooldownMs / 1000
                                    gatePasswordError = String.format(lockedOutStr, seconds)
                                }
                            }
                        }
                    },
                    errorMessage = gatePasswordError
                )
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
                            String.format(
                                stringResource(R.string.picker_preparing), done, total
                            )
                        } else stringResource(R.string.picker_preparing_generic),
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
                    text = stringResource(R.string.locked),
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
                    text = stringResource(R.string.locked_album_security_error),
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
                    text = stringResource(android.R.string.ok)
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
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: androidx.compose.animation.AnimatedContentScope
) {
    with(sharedTransitionScope) {
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
                        AlbumImage(
                            album = album,
                            isEnabled = true,
                            onItemClick = onAlbumClick,
                            onItemLongClick = null
                        )
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
                        label = stringResource(R.string.security_private_folder),
                        onClick = onPrivateFolderClick
                    )
                }
            }
            if (showCloud) {
                item(key = "cloud_source") {
                    PickerSourceCard(
                        icon = Icons.Outlined.Cloud,
                        label = stringResource(R.string.cloud),
                        onClick = onCloudClick
                    )
                }
            }
            if (showVault) {
                item(key = "vault_source") {
                    PickerSourceCard(
                        icon = Icons.Outlined.Lock,
                        label = stringResource(R.string.vault),
                        onClick = onVaultClick
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerSourceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
