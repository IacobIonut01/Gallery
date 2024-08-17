@file:Suppress("LABEL_NAME_CLASH")

package com.dot.gallery.feature_node.presentation.vault

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.EncryptedMediaState
import com.dot.gallery.feature_node.domain.model.EncryptedMedia
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.use_case.VaultUseCases
import com.dot.gallery.feature_node.presentation.util.RepeatOnResume
import com.dot.gallery.feature_node.presentation.util.collectEncryptedMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@HiltViewModel
open class VaultViewModel @Inject constructor(
    private val contentResolver: ContentResolver,
    private val vaultUseCases: VaultUseCases
) : ViewModel() {

    private var _currentVault = MutableStateFlow<Vault?>(null)
    val currentVault = _currentVault.asStateFlow()

    private val _mediaState = MutableStateFlow(EncryptedMediaState())
    val mediaState = _mediaState.asStateFlow()

    private val _vaults = MutableStateFlow<List<Vault>>(emptyList())
    val vaults = _vaults.asStateFlow()

    init {
        initVaults()
    }

    fun setVault(vault: Vault?, onFailed: (reason: String) -> Unit) {
        viewModelScope.launch {
            vaultUseCases.getVaults().collectLatest { vaults ->
                if (vault == null) {
                    getMedia(null)
                    return@collectLatest
                }
                val hasVault = vaults.find { it.uuid == vault.uuid } != null
                if (hasVault) {
                    getMedia(vault)
                } else {
                    vaultUseCases.createVault(
                        vault = vault,
                        onSuccess = {
                            getMedia(vault)
                        },
                        onFailed = onFailed
                    )
                }
            }
        }
    }

    fun deleteVault(vault: Vault) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultUseCases.deleteVault(
                vault = vault,
                onSuccess = {
                    getMedia(null)
                },
                onFailed = {
                    getMedia(null)
                }
            )
        }
    }

    /**
     * Attach the [VaultViewModel] to the lifecycle of the composable.
     * This will start watching the directory for changes and update the media state accordingly.
     * It will also fetch the media when the composable is resumed.
     * This should be called in the composable that will use the [VaultViewModel].
     *
     * Call only after Biometric Auth is successful.
     */
    @SuppressLint("ComposableNaming")
    @Composable
    fun attachToLifecycle() {
        LaunchedEffect(Unit) {
            getMedia(_currentVault.value)
        }
    }

    fun addMedia(media: Media) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _currentVault.value?.let { vault ->
                    getBytes(media.uri)?.let {
                        vaultUseCases.addMedia(vault, media)
                    } ?: return@let
                    getMedia(vault)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun restoreMedia(vault: Vault, media: EncryptedMedia) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultUseCases.restoreMedia(vault, media)
            getMedia(vault)
        }
    }

    fun deleteMedia(vault: Vault, media: EncryptedMedia) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultUseCases.deleteEncryptedMedia(vault, media)
            getMedia(vault)
        }
    }

    fun deleteAllMedia(vault: Vault) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultUseCases.deleteAllEncryptedMedia(
                vault = vault,
                onSuccess = {
                    getMedia(vault)
                },
                onFailed = { failedFiles ->
                    // TODO: Handle failed files
                }
            )
        }
    }

    private fun initVaults() {
        viewModelScope.launch(Dispatchers.IO) {
            vaultUseCases.getVaults().collectLatest { vaults ->
                _vaults.emit(vaults)
            }
        }
    }

    @Throws(IOException::class)
    private fun getBytes(uri: Uri): ByteArray? =
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val byteBuffer = ByteArrayOutputStream()
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)

            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            byteBuffer.toByteArray()
        }


    private fun getMedia(vault: Vault?) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultUseCases.getVaults().collectLatest { vaults ->
                _vaults.emit(vaults)
                if (vaults.isEmpty()) {
                    _mediaState.emit(EncryptedMediaState(isLoading = false, error = "No vaults found"))
                    return@collectLatest
                }
                _currentVault.emit(vault ?: vaults.first())
                vaultUseCases.getEncryptedMedia(_currentVault.value!!).collectLatest {
                    val data = it.data ?: emptyList()
                    if (_mediaState.value.media == data) {
                        if (data.isEmpty()) {
                            _mediaState.emit(EncryptedMediaState(isLoading = false))
                        }
                        return@collectLatest
                    }
                    _mediaState.collectEncryptedMedia(data = data)
                }
            }
        }
    }

    override fun onCleared() {
        _mediaState.value = EncryptedMediaState()
        _currentVault.value = null
        super.onCleared()
    }
}