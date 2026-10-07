/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library.location

import android.content.Context
import com.dot.gallery.cloud.core.CloudMapMarker
import com.dot.gallery.cloud.core.ConnectionState
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.MapCapableProvider
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.data.dao.CloudServerConfigDao
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.domain.model.GeoMedia
import com.dot.gallery.domain.model.LocationMedia
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.model.MediaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.domain.uri.query
import com.dot.gallery.domain.model.locationIdentityKey
import com.dot.gallery.domain.model.matchesLocationCoordinates
import com.dot.gallery.core.logging.printError
import com.dot.gallery.domain.model.locationCoordinateGroupKey
import com.dot.gallery.domain.model.definedTimestamp
import com.dot.gallery.feature.library.generated.resources.Res
import com.dot.gallery.feature.library.generated.resources.unknown_location
import org.jetbrains.compose.resources.getString

data class AccountCloudMapMarker(
    val configId: Long,
    val marker: CloudMapMarker,
) {
    val key: CloudMapAssetKey
        get() = CloudMapAssetKey(marker.providerType, configId, marker.assetId)
}