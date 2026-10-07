/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.app.presentation.privatefolder

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Settings.Misc.rememberGridSize
import com.dot.gallery.core.navigate
import com.dot.gallery.core.navigateUp
import com.dot.gallery.core.presentation.components.SelectionSheet
import com.dot.gallery.data.model.MediaMetadataState
import com.dot.gallery.feature.vault.domain.model.GateMode
import com.dot.gallery.feature.vault.domain.util.VaultGatePolicy
import com.dot.gallery.feature.vault.ui.PrivateFolderContent
import com.dot.gallery.feature.vault.ui.components.VaultDeviceAuthBridge
import com.dot.gallery.core.presentation.util.Screen
import com.dot.gallery.feature.vault.utils.VaultPasswordManager
import com.dot.gallery.feature.vault.utils.rememberBiometricState
import com.dot.gallery.injection.injectViewModel
import com.dot.gallery.feature.vault.privatefolder.PrivateFolderViewModel
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.private_folder_unlock
import com.dot.gallery.generated.resources.private_folder_unlock_subtitle

/**
 * Private-folder media grid behind a memory-only auth gate. UI lives in
 * `:feature:vault` ([PrivateFolderContent]); credential storage
 * ([VaultPasswordManager]), the BiometricPrompt and process lifecycle stay
 * app-side here.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PrivateFolderScreen(
    paddingValues: PaddingValues,
    isScrolling: MutableState<Boolean>,
    metadataState: State<MediaMetadataState>,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    val context = LocalContext.current
    val eventHandler = LocalEventHandler.current

    // One-shot result state consumed by the shared content through the bridge.
    val deviceOutcome = remember { mutableStateOf<Boolean?>(null) }
    val biometricState = rememberBiometricState(
        title = stringResource(Res.string.private_folder_unlock),
        subtitle = stringResource(Res.string.private_folder_unlock_subtitle),
        onSuccess = { deviceOutcome.value = true },
        onFailed = { deviceOutcome.value = false }
    )
    val deviceAuth = remember(biometricState) {
        VaultDeviceAuthBridge(
            isSupported = biometricState.isSupported,
            authenticateAction = { biometricState.authenticate() },
            cancelAction = { biometricState.cancelAuthentication() },
            outcomeState = deviceOutcome
        )
    }

    PrivateFolderContent(
        paddingValues = paddingValues,
        isScrolling = isScrolling,
        metadataState = metadataState,
        gridSizeIndex = rememberGridSize(),
        privateFolderAlbumId = PrivateFolderViewModel.PRIVATE_FOLDER_ALBUM_ID,
        lifecycleOwner = ProcessLifecycleOwner.get(),
        deviceAuth = deviceAuth,
        resolveGateAction = {
            val mode = VaultPasswordManager.getPrivateFolderMode(context)
            val credential = if (mode == GateMode.CUSTOM) {
                VaultPasswordManager.getCredentialStatus(
                    context,
                    VaultPasswordManager.PRIVATE_FOLDER_UUID
                )
            } else null
            VaultGatePolicy.resolve(mode, deviceAuth.isSupported, credential)
        },
        verifyPassword = { secret ->
            VaultPasswordManager.verifyPassword(
                context,
                VaultPasswordManager.PRIVATE_FOLDER_UUID,
                secret
            )
        },
        onNavigateUp = eventHandler::navigateUp,
        onOpenSecuritySettings = {
            eventHandler.navigate(Screen.PrivateFolderSecurityScreen())
        },
        onMediaClick = { media ->
            eventHandler.navigate(
                Screen.MediaViewScreen.idAndAlbum(
                    media.id,
                    PrivateFolderViewModel.PRIVATE_FOLDER_ALBUM_ID
                )
            )
        },
        mediaStateProvider = {
            injectViewModel<PrivateFolderViewModel> {privateFolderViewModel}.mediaState.collectAsStateWithLifecycle()
        },
        selectionSheet = { mediaState, selectedMediaList ->
            SelectionSheet(
                modifier = Modifier.align(Alignment.BottomEnd),
                allMedia = mediaState,
                selectedMedia = selectedMediaList,
                isInPrivateFolder = true
            )
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope
    )
}
