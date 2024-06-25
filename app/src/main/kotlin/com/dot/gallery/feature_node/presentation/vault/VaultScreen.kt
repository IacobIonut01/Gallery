package com.dot.gallery.feature_node.presentation.vault

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.feature_node.presentation.vault.utils.rememberBiometricState

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VaultScreen(
    shouldSkipAuth: MutableState<Boolean>,
    navigateUp: () -> Unit,
    navigate: (route: String) -> Unit,
    vm: VaultViewModel
) {
    val window = (LocalContext.current as Activity).window

    DisposableEffect(Unit) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val vaults by vm.vaults.collectAsStateWithLifecycle()
    val currentVault by vm.currentVault.collectAsStateWithLifecycle()

    var isAuthenticated by remember { mutableStateOf(shouldSkipAuth.value) }
    val biometricState = rememberBiometricState(
        onSuccess = {
            isAuthenticated = true
        },
        onFailed = { isAuthenticated = false },
        biometricPromptInfo = PromptInfo.Builder()
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setTitle(stringResource(R.string.biometric_authentication))
            .setSubtitle(stringResource(R.string.unlock_your_vault))
            .build()
    )
    var addNewVault by remember { mutableStateOf(false) }
    val canAllowAccess = remember(biometricState) {
        biometricState.canAllowAccess
    }

    LaunchedEffect(isAuthenticated, canAllowAccess, vaults) {
        if (!isAuthenticated && !addNewVault && vaults.isNotEmpty()) {
            if (canAllowAccess) {
                biometricState.authenticate()
            } else navigateUp()
        }
    }
    if (isAuthenticated && canAllowAccess) {
        vm.attachToLifecycle()
    }

    AnimatedVisibility(
        visible = addNewVault || vaults.isEmpty(),
        enter = enterAnimation,
        exit = exitAnimation
    ) {
        VaultSetup(
            navigateUp = {
                if (addNewVault) {
                    addNewVault = false
                    if (vaults.isEmpty()) navigateUp()
                } else {
                    navigateUp()
                }
            },
            onCreate = {
                addNewVault = false
                isAuthenticated = false
                biometricState.authenticate()
            },
            vm = vm
        )
    }

    AnimatedVisibility(
        visible = currentVault != null && !addNewVault && isAuthenticated,
        enter = enterAnimation,
        exit = exitAnimation
    ) {
        VaultDisplay(
            navigateUp = navigateUp,
            navigate = navigate,
            onCreateVaultClick = { addNewVault = true },
            vm = vm
        )
    }
}

