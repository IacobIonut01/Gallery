package com.dot.gallery.feature_node.presentation.vault

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.DecryptedMedia
import com.dot.gallery.feature_node.domain.model.EncryptedMediaState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.model.VaultState
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.util.collectEncryptedMedia
import com.dot.gallery.feature_node.presentation.util.printError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@HiltViewModel
open class VaultViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    var currentVault = mutableStateOf<Vault?>(null)

    private val _mediaState = MutableStateFlow(EncryptedMediaState())
    val mediaState = _mediaState.asStateFlow()

    private val _vaultState = MutableStateFlow(VaultState())
    val vaultState = _vaultState.asStateFlow()

    @SuppressLint("ComposableNaming")
    @Composable
    fun attachToLifecycle() {
        LaunchedEffect(Unit) {
            fetchVaultsAndMedia()
        }
    }

    fun setVault(vault: Vault?, onFailed: (reason: String) -> Unit = {}, onSuccess: () -> Unit) {
        if (vault != currentVault.value) {
            _mediaState.value = EncryptedMediaState()
        }
        viewModelScope.launch(Dispatchers.IO) {
            val newVaultState = repository.getVaults().singleOrNull().mapToVaultState()
            _vaultState.emit(newVaultState)
        }
        viewModelScope.launch(Dispatchers.IO) {
            currentVault.value = vault
            if (vault == null) {
                fetchVaultsAndMedia()
                withContext(Dispatchers.Main.immediate) { onSuccess() }
                return@launch
            }
            val hasVault = _vaultState.value.vaults.find { it.uuid == vault.uuid } != null
            if (hasVault) {
                fetchVaultsAndMedia(vault)
                withContext(Dispatchers.Main.immediate) { onSuccess() }
            } else {
                if (_vaultState.value.vaults.firstOrNull { it.name == vault.name } != null) {
                    onFailed("Already exists")
                    return@launch
                }
                repository.createVault(
                    vault = vault,
                    onSuccess = {
                        fetchVaultsAndMedia(vault)
                        currentVault.value = vault
                        viewModelScope.launch(Dispatchers.Main.immediate) { onSuccess() }
                    },
                    onFailed = onFailed
                )
            }
        }
    }

    fun deleteVault(vault: Vault) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVault(
                vault = vault,
                onSuccess = {
                    fetchVaultsAndMedia()
                },
                onFailed = {
                    printError("Failed to delete vault: $it")
                    fetchVaultsAndMedia(vault)
                }
            )
        }
    }

    fun addMedia(vault: Vault, media: Media) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.addMedia(vault, media)
                fetchVaultsAndMedia(vault)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun restoreMedia(vault: Vault, media: DecryptedMedia, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreMedia(vault, media)
            onSuccess()
            fetchVaultsAndMedia(vault)
        }
    }

    fun deleteMedia(vault: Vault, media: DecryptedMedia, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEncryptedMedia(vault, media)
            onSuccess()
            fetchVaultsAndMedia(vault)
        }
    }

    fun deleteAllMedia(vault: Vault) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllEncryptedMedia(
                vault = vault,
                onSuccess = {
                    fetchVaultsAndMedia(vault)
                },
                onFailed = { failedFiles ->
                    printError("Failed to delete files: $failedFiles")
                    // TODO: Handle failed files
                }
            )
        }
    }

    private fun fetchVaultsAndMedia(vault: Vault? = null) {
        currentVault.value = vault
        viewModelScope.launch(Dispatchers.IO) {
            repository.getVaults().collectLatest { resource ->
                _vaultState.emit(resource.mapToVaultState())
            }
        }
        vault?.let {
            viewModelScope.launch(Dispatchers.IO) {
                repository.getEncryptedMedia(vault).collectLatest {
                    _mediaState.collectEncryptedMedia(data = it.data ?: emptyList())
                }
            }
        }
    }

    private fun Resource<List<Vault>>?.mapToVaultState(): VaultState {
        return VaultState(
            isLoading = false,
            vaults = (this?.data) ?: emptyList()
        )
    }

    override fun onCleared() {
        _mediaState.value = EncryptedMediaState()
        super.onCleared()
    }
}