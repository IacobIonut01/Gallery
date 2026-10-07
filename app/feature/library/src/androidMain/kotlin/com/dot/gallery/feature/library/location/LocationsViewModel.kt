package com.dot.gallery.feature.library.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dot.gallery.core.MediaDistributor
import com.dot.gallery.domain.model.MediaState
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.dot.gallery.domain.model.matchesLocationName
import com.dot.gallery.domain.model.matchesLocationCoordinates
import com.dot.gallery.domain.model.definedTimestamp



class LocationsViewModel @javax.inject.Inject constructor(
    mediaDistributor: MediaDistributor,
    mapGeoMediaSource: MapGeoMediaSource,
    @me.tatarka.inject.annotations.Assisted private val gpsLocationNameCity: String,
    @me.tatarka.inject.annotations.Assisted private val gpsLocationNameCountry: String,
    @me.tatarka.inject.annotations.Assisted private val latitude: Double?,
    @me.tatarka.inject.annotations.Assisted private val longitude: Double?,
) : ViewModel() {


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