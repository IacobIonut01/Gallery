/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.setup.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.presentation.components.SetupButton
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.dot.gallery.feature.setup.generated.resources.Res
import com.dot.gallery.feature.setup.generated.resources.access_media_location
import com.dot.gallery.feature.setup.generated.resources.granted
import com.dot.gallery.feature.setup.generated.resources.internet
import com.dot.gallery.feature.setup.generated.resources.permission_local_network_title
import com.dot.gallery.feature.setup.generated.resources.permission_manage_files_title
import com.dot.gallery.feature.setup.generated.resources.permission_manage_media_title
import com.dot.gallery.feature.setup.generated.resources.post_notifications
import com.dot.gallery.feature.setup.generated.resources.read_media_images
import com.dot.gallery.feature.setup.generated.resources.read_media_videos
import com.dot.gallery.feature.setup.generated.resources.setup_continue
import com.dot.gallery.feature.setup.generated.resources.setup_grant_permissions
import com.dot.gallery.feature.setup.generated.resources.setup_media_access_full
import com.dot.gallery.feature.setup.generated.resources.setup_media_access_limited
import com.dot.gallery.feature.setup.generated.resources.setup_open_app_settings
import com.dot.gallery.feature.setup.generated.resources.setup_permission_not_granted
import com.dot.gallery.feature.setup.generated.resources.setup_permission_optional_not_granted
import com.dot.gallery.feature.setup.generated.resources.setup_permissions_subtitle
import com.dot.gallery.feature.setup.generated.resources.setup_permissions_title
import com.dot.gallery.feature.setup.generated.resources.setup_reason_internet
import com.dot.gallery.feature.setup.generated.resources.setup_reason_local_network
import com.dot.gallery.feature.setup.generated.resources.setup_reason_manage_files
import com.dot.gallery.feature.setup.generated.resources.setup_reason_manage_media
import com.dot.gallery.feature.setup.generated.resources.setup_reason_media_location_optional
import com.dot.gallery.feature.setup.generated.resources.setup_reason_notifications
import com.dot.gallery.feature.setup.generated.resources.setup_reason_read_images
import com.dot.gallery.feature.setup.generated.resources.setup_reason_read_videos


/**
 * Permissions page of the setup wizard — pure display + callbacks; the app shell owns
 * the runtime permission launchers and decides which rows exist for this SDK/build.
 */
@Composable
fun SetupPermissionsContent(
    state: SetupPermissionsState,
    actions: SetupPermissionsActions,
    stepNumber: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SetupWizardScaffold(
        modifier = modifier,
        showBack = true,
        onBack = onBack,
        stepNumber = stepNumber,
        totalSteps = totalSteps,
        title = stringResource(Res.string.setup_permissions_title),
        subtitle = stringResource(Res.string.setup_permissions_subtitle),
        bottomBar = {
            if (state.coreGranted) {
                SetupButton(
                    text = stringResource(Res.string.setup_continue),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    applyNavigationPadding = false,
                    onClick = { if (actions.recheckMediaAccess()) onNext() }
                )
            } else {
                SetupButton(
                    text = stringResource(Res.string.setup_grant_permissions),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    applyNavigationPadding = false,
                    onClick = actions.onRequestCorePermissions
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
            state.permissions.forEach { item ->
                SetupPermissionRow(item = item, mediaAccess = state.mediaAccess, onGrant = actions.onGrant)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SetupPermissionRow(
    item: SetupPermissionUi,
    mediaAccess: SetupMediaAccess,
    onGrant: (SetupPermission) -> Unit,
) {
    when (item.permission) {
        SetupPermission.IMAGES -> SetupPermissionItem(
            icon = Icons.Rounded.Image,
            title = stringResource(Res.string.read_media_images),
            reason = stringResource(Res.string.setup_reason_read_images),
            granted = item.granted,
            statusLabel = stringResource(mediaAccess.labelRes()),
        )

        SetupPermission.VIDEO -> SetupPermissionItem(
            icon = Icons.Rounded.VideoFile,
            title = stringResource(Res.string.read_media_videos),
            reason = stringResource(Res.string.setup_reason_read_videos),
            granted = item.granted,
            statusLabel = stringResource(mediaAccess.labelRes()),
        )

        SetupPermission.MEDIA_LOCATION -> SetupPermissionItem(
            icon = Icons.Rounded.LocationOn,
            title = stringResource(Res.string.access_media_location),
            reason = stringResource(Res.string.setup_reason_media_location_optional),
            optional = true,
            granted = item.granted,
            statusLabel = stringResource(
                if (item.granted) Res.string.granted else Res.string.setup_permission_optional_not_granted
            ),
            grantActionLabel = stringResource(
                if (item.requestAttempted && !item.granted) {
                    Res.string.setup_open_app_settings
                } else {
                    Res.string.setup_grant_permissions
                }
            ),
            onGrant = { onGrant(item.permission) }
        )

        SetupPermission.INTERNET -> SetupPermissionItem(
            icon = Icons.Rounded.SignalWifi4Bar,
            title = stringResource(Res.string.internet),
            reason = stringResource(Res.string.setup_reason_internet)
        )

        SetupPermission.LOCAL_NETWORK -> SetupPermissionItem(
            icon = Icons.Rounded.SignalWifi4Bar,
            title = stringResource(Res.string.permission_local_network_title),
            reason = stringResource(Res.string.setup_reason_local_network),
            optional = true,
            granted = item.granted,
            grantActionLabel = stringResource(Res.string.setup_grant_permissions),
            onGrant = { onGrant(item.permission) }
        )

        SetupPermission.NOTIFICATIONS -> SetupPermissionItem(
            icon = Icons.Rounded.Notifications,
            title = stringResource(Res.string.post_notifications),
            reason = stringResource(Res.string.setup_reason_notifications),
            optional = true,
            granted = item.granted,
            grantActionLabel = stringResource(Res.string.setup_grant_permissions),
            onGrant = { onGrant(item.permission) }
        )

        SetupPermission.MANAGE_MEDIA -> SetupPermissionItem(
            icon = Icons.Rounded.PermMedia,
            title = stringResource(Res.string.permission_manage_media_title),
            reason = stringResource(Res.string.setup_reason_manage_media),
            optional = true,
            granted = item.granted,
            grantActionLabel = stringResource(Res.string.setup_grant_permissions),
            onGrant = { onGrant(item.permission) }
        )

        SetupPermission.MANAGE_FILES -> SetupPermissionItem(
            icon = Icons.Rounded.FileOpen,
            title = stringResource(Res.string.permission_manage_files_title),
            reason = stringResource(Res.string.setup_reason_manage_files),
            optional = true,
            granted = item.granted,
            grantActionLabel = stringResource(Res.string.setup_grant_permissions),
            onGrant = { onGrant(item.permission) }
        )
    }
}

private fun SetupMediaAccess.labelRes(): StringResource = when (this) {
    SetupMediaAccess.FULL -> Res.string.setup_media_access_full
    SetupMediaAccess.LIMITED -> Res.string.setup_media_access_limited
    SetupMediaAccess.NONE -> Res.string.setup_permission_not_granted
}
