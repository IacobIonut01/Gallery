package com.dot.gallery.feature.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.ui.util.rememberAppBottomSheetState
import com.dot.gallery.domain.model.LocationData
import com.dot.gallery.domain.uri.Uri
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.location


/**
 * Location row shown in the media-details sheet. When [mapPreview] is provided a
 * small preview renders in the trailing box and tapping opens the detail sheet;
 * otherwise tapping goes straight to an external map via [onOpenMap].
 */
@Composable
fun LocationItem(
    modifier: Modifier = Modifier,
    iconBackgroundModifier: Modifier = Modifier,
    locationData: LocationData?,
    mediaUri: Uri? = null,
    mapPreview: (@Composable (Modifier) -> Unit)? = null,
    onOpenMap: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    onShowInApp: (() -> Unit)? = null,
) {
    val mapsEnabled = mapPreview != null
    val locationSheetState = rememberAppBottomSheetState()
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = locationData != null,
        enter = enterAnimation,
        exit = exitAnimation
    ) {
        if (locationData != null) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (mapsEnabled) {
                            scope.launch { locationSheetState.show() }
                        } else {
                            onOpenMap(locationData.latitude, locationData.longitude)
                        }
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(iconBackgroundModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.location),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = locationData.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedVisibility(visible = mapsEnabled) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        mapPreview?.invoke(Modifier.fillMaxSize())
                    }
                }
            }

            if (mapsEnabled && locationSheetState.isVisible) {
                LocationDetailSheet(
                    state = locationSheetState,
                    locationData = locationData,
                    mediaUri = mediaUri,
                    mapPreview = mapPreview,
                    onOpenMap = onOpenMap,
                    onShowInApp = onShowInApp ?: {},
                )
            }
        }
    }
}
