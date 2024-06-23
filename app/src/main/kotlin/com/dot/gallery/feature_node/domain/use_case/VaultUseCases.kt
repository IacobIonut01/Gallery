package com.dot.gallery.feature_node.domain.use_case

import com.dot.gallery.feature_node.domain.model.EncryptedMedia
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import kotlinx.coroutines.flow.map
import java.io.File

class VaultUseCases(
    private val repository: MediaRepository
) {

    fun getVaults() = repository.getVaults().map { it.data ?: emptyList() }

    suspend fun createVault(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (reason: String) -> Unit
    ) = repository.createVault(vault, onSuccess, onFailed)

    suspend fun deleteVault(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (reason: String) -> Unit
    ) = repository.deleteVault(vault, onSuccess, onFailed)

    fun getEncryptedMedia(vault: Vault) =
        repository.getEncryptedMedia(vault)

    suspend fun addMedia(vault: Vault, media: Media) =
        repository.addMedia(vault, media)

    suspend fun restoreMedia(vault: Vault, media: EncryptedMedia) =
        repository.restoreMedia(vault, media)

    suspend fun deleteEncryptedMedia(vault: Vault, media: EncryptedMedia) =
        repository.deleteEncryptedMedia(vault, media)

    suspend fun deleteAllEncryptedMedia(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (reason: List<File>) -> Unit
    ) = repository.deleteAllEncryptedMedia(vault, onSuccess, onFailed)

}


