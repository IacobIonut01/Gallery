package com.dot.gallery.feature_node.presentation.vault

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.ui.core.Icons
import com.dot.gallery.ui.core.icons.Encrypted

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VaultSetup(
    navigateUp: () -> Unit,
    onCreate: () -> Unit,
    vm: VaultViewModel
) {
    val context = LocalContext.current

    var nameError by remember { mutableStateOf("") }
    var newVault by remember { mutableStateOf(Vault(name = "")) }

    val biometricManager = remember { BiometricManager.from(context) }
    val isBiometricAvailable = remember {
        biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BIOMETRIC_SUCCESS
    }


    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = navigateUp) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
                Button(
                    onClick = {
                        vm.setVault(newVault) {
                            println("Error: $it")
                            nameError = it
                        }
                        if (nameError.isEmpty()) {
                            onCreate()
                        }
                    },
                    enabled = isBiometricAvailable && nameError.isEmpty() && newVault.name.isNotEmpty()
                ) {
                    Text(text = stringResource(id = R.string.get_started))
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Encrypted,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Setup your vault",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Encrypt your most private photos & videos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = newVault.name,
                onValueChange = { newName ->
                    newVault = newVault.copy(name = newName)
                },
                label = { Text(text = "Vault Name") },
                isError = nameError.isNotEmpty(),
                enabled = isBiometricAvailable
            )
            AnimatedVisibility(visible = nameError.isNotEmpty()) {
                Text(
                    text = nameError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            AnimatedVisibility(visible = !isBiometricAvailable) {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    text = "Please set-up a phone security measure before setting up the vault.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

            }

            AnimatedVisibility(visible = isBiometricAvailable) {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    text = "The vault will be accessed using your phone security measures (password or biometrics)\n\n" +
                            "Encryption key can be accessed inside the vault and will be used in order to restore any vaults.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}