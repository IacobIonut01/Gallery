package com.dot.gallery.feature_node.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.feature_node.domain.model.GeoMedia
import com.dot.gallery.feature_node.domain.model.MediaState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = LocationsViewModel.Factory::class)
class LocationsViewModel @AssistedInject constructor(
    mediaDistributor: MediaDistributor,
    @Assisted("city") private val gpsLocationNameCity: String,
    @Assisted("country") private val gpsLocationNameCountry: String
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("city") gpsLocationNameCity: String,
            @Assisted("country") gpsLocationNameCountry: String
        ): LocationsViewModel
    }

    val mediaState by lazy {
        mediaDistributor.locationBasedMedia(
            gpsLocationNameCity = gpsLocationNameCity,
            gpsLocationNameCountry = gpsLocationNameCountry
        ).stateIn(viewModelScope, Eagerly, MediaState())
    }

    val latestGeoMedia = mediaDistributor.geoMediaFlow
        .map { list ->
            list.asSequence()
                .filter { it.locationCity == gpsLocationNameCity && it.locationCountry == gpsLocationNameCountry }
                .maxByOrNull { it.media.definedTimestamp }
        }
        .stateIn(viewModelScope, Eagerly, null)

}