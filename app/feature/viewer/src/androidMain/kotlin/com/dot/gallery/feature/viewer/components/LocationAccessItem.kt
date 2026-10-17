package com.dot.gallery.feature.viewer.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.location
import com.dot.gallery.feature.viewer.generated.resources.location_access_row_hint
import org.jetbrains.compose.resources.stringResource



/**
 * Shown in place of the location row when a local image reports no coordinates and
 * ACCESS_MEDIA_LOCATION is missing — without it MediaProvider serves EXIF with the
 * GPS ranges zeroed, so the most likely reason there is nothing to show is redaction.
 * Taps re-request until the system considers the permission permanently denied
 * (rationale no longer shown), then escalate to the app details settings page.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationAccessItem(
    modifier: Modifier = Modifier,
    iconBackgroundModifier: Modifier = Modifier,
    onGranted: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    val context = LocalContext.current
    var requestAttempted by rememberSaveable { mutableStateOf(false) }
    var requestInFlight by remember { mutableStateOf(false) }
    val locationPermission = rememberPermissionState(
        Manifest.permission.ACCESS_MEDIA_LOCATION
    ) {
        requestInFlight = false
    }
    val latestOnGranted by rememberUpdatedState(onGranted)

    // The accompanist result callback only reports the in-app request — a grant
    // made from the app-details settings page flips the status on resume with no
    // callback at all, which used to leave the location empty until the next
    // metadata scan. Drive the refresh off the status transition itself.
    var grantReported by remember { mutableStateOf(locationPermission.status.isGranted) }
    LaunchedEffect(locationPermission.status) {
        if (!grantReported && locationPermission.status.isGranted) {
            grantReported = true
            latestOnGranted()
        }
    }

    AnimatedVisibility(
        visible = !locationPermission.status.isGranted,
        enter = enterAnimation,
        exit = exitAnimation
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    if (requestInFlight) return@clickable
                    if (shouldOpenPermissionSettings(requestAttempted, locationPermission.status)) {
                        context.startActivity(
                            Intent(
                                AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    } else {
                        requestAttempted = true
                        requestInFlight = true
                        // Accompanist throws IllegalStateException when its launcher is
                        // still null — without the catch the guard would jam the row dead.
                        try {
                            locationPermission.launchPermissionRequest()
                        } catch (e: IllegalStateException) {
                            requestInFlight = false
                        }
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
                    text = stringResource(Res.string.location_access_row_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Whether a tap on the prompt should open the app-details settings page instead of
 * re-requesting. Escalate only once the system will no longer show the dialog —
 * [PermissionStatus.Denied] with `shouldShowRationale == false` — and only after at
 * least one request was made (a fresh denial before any request is indistinguishable
 * from a permanent one). An attempt that never reached a result — auto-grant flash,
 * process death while the dialog was up — must not send later taps to settings.
 */
@OptIn(ExperimentalPermissionsApi::class)
fun shouldOpenPermissionSettings(requestAttempted: Boolean, status: PermissionStatus): Boolean =
    requestAttempted && status is PermissionStatus.Denied && !status.shouldShowRationale
