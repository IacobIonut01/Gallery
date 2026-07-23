/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun GalleryMapView(
    modifier: Modifier = Modifier,
    mapState: GalleryMapState,
    styleUri: String,
    isLogoEnabled: Boolean = true,
    isAttributionEnabled: Boolean = false,
    isCompassEnabled: Boolean = false,
    isScaleBarEnabled: Boolean = false,
    onMapClick: ((LatLng) -> Boolean)? = null,
    onCameraMoved: ((GalleryCameraPosition) -> Unit)? = null,
    onUserInteraction: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnMapClick = rememberUpdatedState(onMapClick)
    val currentOnCameraMoved = rememberUpdatedState(onCameraMoved)
    val currentOnUserInteraction = rememberUpdatedState(onUserInteraction)

    // Remember the MapView — call onCreate immediately so the native
    // renderer initialises without waiting for a lifecycle event.
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { mv ->
            mv.onCreate(null)
            mapState.mapView = mv
        }
    }

    LaunchedEffect(styleUri) {
        mapState.loadStyle(styleUri)
    }

    DisposableEffect(mapView) {
        mapView.getMapAsync { mapLibreMap ->
            // Ornament settings
            mapLibreMap.uiSettings.apply {
                this.isLogoEnabled = isLogoEnabled
                this.isAttributionEnabled = isAttributionEnabled
                this.isCompassEnabled = isCompassEnabled
            }

            // Set initial camera
            mapLibreMap.cameraPosition = mapState.cameraPosition.toNative()

            mapState.attachMap(mapLibreMap)

            // Camera listener — sync back to Compose state
            mapLibreMap.addOnCameraMoveListener {
                val pos = mapLibreMap.cameraPosition
                val galleryPos = GalleryCameraPosition(
                    latitude = pos.target?.latitude ?: 0.0,
                    longitude = pos.target?.longitude ?: 0.0,
                    zoom = pos.zoom,
                    tilt = pos.tilt,
                    bearing = pos.bearing,
                )
                mapState.cameraPosition = galleryPos
                currentOnCameraMoved.value?.invoke(galleryPos)
            }

            // Detect user-initiated gestures (pan, zoom, rotate)
            mapLibreMap.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    currentOnUserInteraction.value?.invoke()
                }
            }

            mapLibreMap.addOnMapClickListener { latLng ->
                currentOnMapClick.value?.invoke(latLng) ?: false
            }
        }

        onDispose { }
    }

    // Lifecycle management — catch up with current state then observe future events.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    mapState.onDestroy()
                    mapView.onDestroy()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // The composable likely enters when the lifecycle is already RESUMED,
        // so the observer above would never get ON_START / ON_RESUME.
        // Catch up manually so the render thread starts immediately.
        val currentState = lifecycleOwner.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
    )
}
