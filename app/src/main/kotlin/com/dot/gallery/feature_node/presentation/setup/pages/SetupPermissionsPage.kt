/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.setup.pages

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dot.gallery.BuildConfig
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.presentation.setup.components.SetupPermissionItem
import com.dot.gallery.feature_node.presentation.setup.components.SetupWizardScaffold
import com.dot.gallery.feature_node.presentation.util.RepeatOnResume
import com.dot.gallery.feature_node.presentation.util.isManageFilesAllowed
import com.dot.gallery.feature_node.presentation.util.launchManageFiles
import com.dot.gallery.feature_node.presentation.util.launchManageMedia
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SetupPermissionsPage(
    stepNumber: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mediaPermissions = rememberMultiplePermissionsState(Constants.PERMISSIONS)
    val coreGranted = mediaPermissions.allPermissionsGranted

    SetupWizardScaffold(
        showBack = true,
        onBack = onBack,
        stepNumber = stepNumber,
        totalSteps = totalSteps,
        title = stringResource(R.string.setup_permissions_title),
        subtitle = stringResource(R.string.setup_permissions_subtitle),
        bottomBar = {
            if (coreGranted) {
                SetupButton(
                    text = stringResource(R.string.setup_continue),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    applyNavigationPadding = false,
                    onClick = onNext
                )
            } else {
                SetupButton(
                    text = stringResource(R.string.setup_grant_permissions),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    applyNavigationPadding = false,
                    onClick = {
                        scope.launch { mediaPermissions.launchMultiplePermissionRequest() }
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Required permissions
            SetupPermissionItem(
                icon = Icons.Rounded.Image,
                title = stringResource(R.string.read_media_images),
                reason = stringResource(R.string.setup_reason_read_images)
            )
            SetupPermissionItem(
                icon = Icons.Rounded.VideoFile,
                title = stringResource(R.string.read_media_videos),
                reason = stringResource(R.string.setup_reason_read_videos)
            )
            SetupPermissionItem(
                icon = Icons.Rounded.LocationOn,
                title = stringResource(R.string.access_media_location),
                reason = stringResource(R.string.setup_reason_media_location)
            )
            if (!BuildConfig.OFFLINE_MODE) {
                SetupPermissionItem(
                    icon = Icons.Rounded.SignalWifi4Bar,
                    title = stringResource(R.string.internet),
                    reason = stringResource(R.string.setup_reason_internet)
                )
            }

            // Optional permissions (manual grant + verified state)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                var isStorageManager by remember { mutableStateOf(Environment.isExternalStorageManager()) }
                var canManageMedia by remember { mutableStateOf(MediaStore.canManageMedia(context)) }
                var notificationsGranted by remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                var localNetworkGranted by remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT < 37 ||
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_LOCAL_NETWORK
                            ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                RepeatOnResume {
                    isStorageManager = Environment.isExternalStorageManager()
                    canManageMedia = MediaStore.canManageMedia(context)
                    if (Build.VERSION.SDK_INT >= 37) {
                        localNetworkGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_LOCAL_NETWORK
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                }

                if (Build.VERSION.SDK_INT >= 37 && !BuildConfig.OFFLINE_MODE) {
                    val localNetworkPermission = rememberPermissionState(
                        permission = Manifest.permission.ACCESS_LOCAL_NETWORK,
                        onPermissionResult = { localNetworkGranted = it }
                    )
                    SetupPermissionItem(
                        icon = Icons.Rounded.SignalWifi4Bar,
                        title = stringResource(R.string.permission_local_network_title),
                        reason = stringResource(R.string.setup_reason_local_network),
                        optional = true,
                        granted = localNetworkGranted,
                        grantActionLabel = stringResource(R.string.setup_grant_permissions),
                        onGrant = { localNetworkPermission.launchPermissionRequest() }
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermission = rememberPermissionState(
                        permission = Manifest.permission.POST_NOTIFICATIONS,
                        onPermissionResult = { notificationsGranted = it }
                    )
                    SetupPermissionItem(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.post_notifications),
                        reason = stringResource(R.string.setup_reason_notifications),
                        optional = true,
                        granted = notificationsGranted,
                        grantActionLabel = stringResource(R.string.setup_grant_permissions),
                        onGrant = { notificationPermission.launchPermissionRequest() }
                    )
                }

                SetupPermissionItem(
                    icon = Icons.Rounded.PermMedia,
                    title = stringResource(R.string.permission_manage_media_title),
                    reason = stringResource(R.string.setup_reason_manage_media),
                    optional = true,
                    granted = canManageMedia,
                    grantActionLabel = stringResource(R.string.setup_grant_permissions),
                    onGrant = { scope.launch { context.launchManageMedia() } }
                )

                if (isManageFilesAllowed) {
                    SetupPermissionItem(
                        icon = Icons.Rounded.FileOpen,
                        title = stringResource(R.string.permission_manage_files_title),
                        reason = stringResource(R.string.setup_reason_manage_files),
                        optional = true,
                        granted = isStorageManager,
                        grantActionLabel = stringResource(R.string.setup_grant_permissions),
                        onGrant = { scope.launch { context.launchManageFiles() } }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
