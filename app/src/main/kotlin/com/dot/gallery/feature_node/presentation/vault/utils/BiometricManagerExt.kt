package com.dot.gallery.feature_node.presentation.vault.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun rememberBiometricManager(): BiometricManager {
    val context = LocalContext.current
    return remember(context) {
        BiometricManager.from(context)
    }
}

@Composable
fun rememberBiometricCallback(
    onSuccess: () -> Unit,
    onFailed: () -> Unit
) = remember {
    object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            onFailed()
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onFailed()
        }
    }
}

@Composable
fun rememberBiometricPrompt(biometricPromptCallback: BiometricPrompt.AuthenticationCallback): BiometricPrompt {
    val context = LocalContext.current
    val executor = remember { ContextCompat.getMainExecutor(context) }

    return remember(context, executor, biometricPromptCallback) {
        BiometricPrompt(
            context as FragmentActivity, executor, biometricPromptCallback
        )
    }
}

@Composable
fun rememberBiometricState(
    biometricPromptInfo: BiometricPrompt.PromptInfo,
    onSuccess: () -> Unit,
    onFailed: () -> Unit
): BiometricState {
    val biometricManager = rememberBiometricManager()
    val callback = rememberBiometricCallback(onSuccess, onFailed)
    val prompt = rememberBiometricPrompt(callback)
    val promptInfo = remember { biometricPromptInfo }
    return remember(biometricManager, biometricPromptInfo) {
        BiometricState(
            biometricManager = biometricManager,
            promptInfo = promptInfo,
            prompt = prompt
        )
    }
}

class BiometricState(
    biometricManager: BiometricManager,
    private val promptInfo: BiometricPrompt.PromptInfo,
    private val prompt: BiometricPrompt
) {
    val canAllowAccess = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BIOMETRIC_SUCCESS

    fun authenticate() {
        if (canAllowAccess) {
            prompt.authenticate(promptInfo)
        }
    }

}