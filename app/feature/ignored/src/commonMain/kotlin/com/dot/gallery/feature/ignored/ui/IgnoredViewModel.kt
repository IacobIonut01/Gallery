package com.dot.gallery.feature.ignored.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.util.MediaOrder
import com.dot.gallery.feature.ignored.ui.IgnoredState
import com.dot.gallery.data.repository.CoreMediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import com.dot.gallery.domain.model.matchesAlbum



class IgnoredViewModel @Inject constructor(
    private val repository: CoreMediaRepository
): ViewModel() {

    val blacklistState = combine(
        repository.getBlacklistedAlbums(),
        repository.getAlbums(MediaOrder.Default)
    ) { ignoredAlbums, albumsResource ->
        val allAlbums = albumsResource.data ?: emptyList()
        val updatedIgnoredAlbums = ignoredAlbums.map { ignored ->
            val wildcard = ignored.wildcard
            if (wildcard != null) {
                try {
                    val regex = wildcard.toRegex()
                    val matchedAlbums = allAlbums.filter(regex::matchesAlbum)
                    ignored.copy(matchedAlbums = matchedAlbums.map { it.label })
                } catch (e: Exception) {
                    ignored
                }
            } else {
                ignored
            }
        }
        IgnoredState(updatedIgnoredAlbums)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), IgnoredState())

    fun removeFromBlacklist(ignoredAlbum: IgnoredAlbum) {
        viewModelScope.launch {
            repository.removeBlacklistedAlbum(ignoredAlbum)
        }
    }
    
    fun updateIgnoredAlbum(ignoredAlbum: IgnoredAlbum) {
        viewModelScope.launch {
            repository.addBlacklistedAlbum(ignoredAlbum)
        }
    }
}