package com.dot.gallery.feature_node.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.feature_node.domain.model.GeoMedia
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.model.matchesLocationCoordinates
import com.dot.gallery.feature_node.domain.model.matchesLocationName
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
    mapGeoMediaSource: MapGeoMediaSource,
    @Assisted("city") private val gpsLocationNameCity: String,
    @Assisted("country") private val gpsLocationNameCountry: String,
    @Assisted("latitude") private val latitude: Double?,
    @Assisted("longitude") private val longitude: Double?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("city") gpsLocationNameCity: String,
            @Assisted("country") gpsLocationNameCountry: String,
            @Assisted("latitude") latitude: Double?,
            @Assisted("longitude") longitude: Double?,
        ): LocationsViewModel
    }

    private val geoMedia = mapGeoMediaSource.mergedGeoMedia(
        localGeoMedia = mediaDistributor.geoMediaFlow,
        timelineMedia = mediaDistributor.timelineMediaFlow,
    ).stateIn(viewModelScope, Eagerly, emptyList())

    private val matchingGeoMediaIds = geoMedia.map { list ->
        list.asSequence()
            .filter {
                matchesLocationName(
                    candidateCity = it.locationCity,
                    candidateCountry = it.locationCountry,
                    city = gpsLocationNameCity,
                    country = gpsLocationNameCountry,
                ) || matchesLocationCoordinates(
                    candidateLatitude = it.latitude,
                    candidateLongitude = it.longitude,
                    latitude = latitude,
                    longitude = longitude,
                )
            }
            .mapTo(HashSet()) { it.mediaId }
    }

    val mediaState by lazy {
        mediaDistributor.locationBasedMedia(
            gpsLocationNameCity = gpsLocationNameCity,
            gpsLocationNameCountry = gpsLocationNameCountry,
            latitude = latitude,
            longitude = longitude,
            additionalMediaIds = matchingGeoMediaIds,
        ).stateIn(viewModelScope, Eagerly, MediaState())
    }

    val latestGeoMedia = geoMedia
        .map { list ->
            list.asSequence()
                .filter {
                    matchesLocationName(
                        candidateCity = it.locationCity,
                        candidateCountry = it.locationCountry,
                        city = gpsLocationNameCity,
                        country = gpsLocationNameCountry,
                    ) || matchesLocationCoordinates(
                        candidateLatitude = it.latitude,
                        candidateLongitude = it.longitude,
                        latitude = latitude,
                        longitude = longitude,
                    )
                }
                .maxByOrNull { it.media.definedTimestamp }
        }
        .stateIn(viewModelScope, Eagerly, null)

}