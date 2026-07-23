/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dot.gallery.R
import com.dot.gallery.cloud.core.CloudUri
import com.dot.gallery.cloud.ui.backup.CloudBackupInfoSheet
import com.dot.gallery.cloud.ui.descriptor.ProviderBrandIcon
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.core.Settings.Misc.rememberAllowBlur
import com.dot.gallery.core.navigate
import com.dot.gallery.core.metadata.MetadataRemovalMode
import com.dot.gallery.core.metadata.SanitizationResult
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.NavigationBarSpacer
import com.dot.gallery.feature_node.domain.model.AlbumState
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaMetadataState
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.model.VaultState
import com.dot.gallery.feature_node.domain.model.rememberLocationData
import com.dot.gallery.feature_node.domain.model.rememberMediaDateCaption
import com.dot.gallery.feature_node.domain.util.canMakeActions
import com.dot.gallery.feature_node.domain.util.fileExtension
import com.dot.gallery.feature_node.domain.util.getCategory
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.domain.util.isCloud
import com.dot.gallery.feature_node.domain.util.isEncrypted
import com.dot.gallery.feature_node.domain.util.isRaw
import com.dot.gallery.feature_node.domain.util.isTrashed
import com.dot.gallery.feature_node.domain.util.isVideo
import com.dot.gallery.feature_node.domain.util.readUriOnly
import com.dot.gallery.feature_node.presentation.exif.MetadataEditSheet
import com.dot.gallery.feature_node.presentation.mediaview.components.media.MotionPhotoShotsSection
import com.dot.gallery.feature_node.presentation.mediaview.components.media.MotionPhotoState
import com.dot.gallery.feature_node.presentation.mediaview.MediaViewViewModel
import com.dot.gallery.feature_node.presentation.mediaview.rememberedDerivedState
import com.dot.gallery.feature_node.presentation.util.GlideInvalidation
import com.dot.gallery.feature_node.presentation.util.LocalHazeState
import com.dot.gallery.feature_node.presentation.util.hazeEffectScaled
import com.dot.gallery.feature_node.presentation.util.Screen
import com.dot.gallery.feature_node.presentation.util.launchWriteRequest
import com.dot.gallery.feature_node.presentation.util.printDebug
import com.dot.gallery.feature_node.presentation.util.rememberActivityResult
import com.dot.gallery.feature_node.presentation.util.rememberAppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.rememberMediaInfo
import com.dot.gallery.feature_node.presentation.util.writeRequest
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch

@OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun <T : Media> MediaViewSheetDetails(
    albumsState: State<AlbumState>,
    vaultState: State<VaultState>,
    metadataState: State<MediaMetadataState>,
    currentMedia: T?,
    restoreMedia: ((Vault, T, () -> Unit) -> Unit)?,
    currentVault: Vault?,
    motionPhotoState: MotionPhotoState? = null,
    cloudBackups: List<Media.UriMedia> = emptyList(),
    metadataSanitizationState: MediaViewViewModel.MetadataSanitizationUiState =
        MediaViewViewModel.MetadataSanitizationUiState.Idle,
    probeMetadataSanitization: (Media) -> Unit = {},
    sanitizeMetadata: (Media, MetadataRemovalMode) -> Unit = { _, _ -> },
    resetMetadataSanitization: () -> Unit = {},
) {
    val metadata by rememberedDerivedState(metadataState.value, currentMedia) {
        currentMedia?.id?.let { metadataState.value.metadataMap[it] }
    }
    LaunchedEffect(metadata) {
        printDebug("Available metadata for ${currentMedia?.id}:\n${metadata.toString()}")
    }
    val handler = LocalMediaHandler.current
    val isBlurEnabled by rememberAllowBlur()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceColorVariant = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val sheetCardBackgroundModifier = remember(isBlurEnabled) {
        if (!isBlurEnabled) {
            Modifier.background(
                color = surfaceColorVariant,
                shape = RoundedCornerShape(16.dp)
            )
        } else {
            Modifier
        }
    }
    val sheetHazeStyle = HazeMaterials.thin(
        containerColor = surfaceColor
    )
    val sheetCardHazeStyle = HazeMaterials.regular(
        containerColor = surfaceColor
    )

    val sheetCardButtonHazeStyle = HazeMaterials.thick(
        containerColor = surfaceColorVariant
    )
    val iconBackgroundHazeStyle = HazeMaterials.thick(
        containerColor = surfaceContainerHigh
    )
    val iconBackgroundModifier = remember(isBlurEnabled) {
        if (!isBlurEnabled) {
            Modifier.background(
                color = surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp)
            )
        } else {
            Modifier
        }
    }
    val buttonBackgroundModifier = remember(isBlurEnabled) {
        if (!isBlurEnabled) {
            Modifier.background(
                color = surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp)
            )
        } else {
            Modifier
        }
    }
    val sheetBackgroundModifier = remember(isBlurEnabled) {
        if (!isBlurEnabled) {
            Modifier.background(
                color = surfaceColor,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
        } else {
            Modifier
        }
    }
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp
                )
            )
            .then(sheetBackgroundModifier)
            .hazeEffectScaled(
                state = LocalHazeState.current,
                style = sheetHazeStyle
            )
            .graphicsLayer {
                translationY = -1f
            }
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DragHandle()
        }

        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth(),
            visible = currentMedia != null && !currentMedia.isTrashed,
            enter = enterAnimation,
            exit = exitAnimation
        ) {
            if (currentMedia != null) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                val metadataRemovalSheetState = rememberAppBottomSheetState()
                var pendingRemovalMode by rememberSaveable(currentMedia.id) {
                    mutableStateOf(MetadataRemovalMode.LOCATION)
                }
                val sanitizationCapability = when (metadataSanitizationState) {
                    is MediaViewViewModel.MetadataSanitizationUiState.Ready ->
                        metadataSanitizationState.takeIf { it.mediaId == currentMedia.id }?.capability
                    else -> null
                }
                val metadataRemovalBusy = when (metadataSanitizationState) {
                    is MediaViewViewModel.MetadataSanitizationUiState.Probing ->
                        metadataSanitizationState.mediaId == currentMedia.id
                    is MediaViewViewModel.MetadataSanitizationUiState.Running ->
                        metadataSanitizationState.mediaId == currentMedia.id
                    else -> false
                }
                val doMetadataRemoval: () -> Unit = {
                    sanitizeMetadata(currentMedia, pendingRemovalMode)
                }
                val metadataRemovalPermissionResult = rememberActivityResult(
                    onResultOk = doMetadataRemoval
                )
                LaunchedEffect(metadataSanitizationState, currentMedia.id) {
                    when (metadataSanitizationState) {
                        is MediaViewViewModel.MetadataSanitizationUiState.Ready -> {
                            if (metadataSanitizationState.mediaId == currentMedia.id) {
                                metadataRemovalSheetState.show()
                            }
                        }
                        is MediaViewViewModel.MetadataSanitizationUiState.Complete -> {
                            if (metadataSanitizationState.mediaId == currentMedia.id) {
                                val result = metadataSanitizationState.result
                                val message = when (result) {
                                    is SanitizationResult.Success -> R.string.remove_metadata_success
                                    is SanitizationResult.CommitFailed -> if (result.rolledBack) {
                                        R.string.remove_metadata_rolled_back
                                    } else {
                                        R.string.remove_metadata_failed
                                    }
                                    else -> R.string.remove_metadata_failed
                                }
                                Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
                                if (result is SanitizationResult.Success) metadataRemovalSheetState.hide()
                                resetMetadataSanitization()
                            }
                        }
                        else -> Unit
                    }
                }

                val dateCaption = rememberMediaDateCaption(metadata, currentMedia)
                val metadataSheetState = rememberAppBottomSheetState()
                val backupSheetState = rememberAppBottomSheetState()
                val allMetadataEventHandler = LocalEventHandler.current
                val mediaInfoList = rememberMediaInfo(
                    media = currentMedia,
                    exifMetadata = metadata,
                    onLabelClick = {
                        if (!currentMedia.readUriOnly) {
                            scope.launch {
                                metadataSheetState.show()
                            }
                        }
                    }
                )

                val locationData = rememberLocationData(metadata)
                var category by remember(currentMedia) {
                    mutableStateOf(currentMedia.getCategory)
                }
                LaunchedEffect(currentMedia, category, handler) {
                    if (category == null) {
                        category = handler.getCategoryForMediaId(currentMedia.id)
                    }
                }
                val mediaCategoryCounter by if (category != null) {
                    handler.getClassifiedMediaCountAtCategory(category!!)
                        .collectAsStateWithLifecycle(0)
                } else {
                    remember { mutableStateOf(0) }
                }
                val mediaCategoryThumbnail by if (category != null) {
                    handler.getClassifiedMediaThumbnailByCategory(category!!)
                        .collectAsStateWithLifecycle(null)
                } else {
                    remember { mutableStateOf(null) }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item(key = "date_location") {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .then(sheetCardBackgroundModifier)
                                .hazeEffectScaled(
                                    state = LocalHazeState.current,
                                    style = sheetCardHazeStyle
                                )
                                .padding(16.dp)
                        ) {
                            DateHeader(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !currentMedia.readUriOnly,
                                        indication = null,
                                        interactionSource = remember {
                                            MutableInteractionSource()
                                        }
                                    ) {
                                        scope.launch {
                                            metadataSheetState.show()
                                        }
                                    },
                                mediaDateCaption = dateCaption
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(state = rememberScrollState())
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (currentMedia.isRaw) {
                                    MediaInfoChip(
                                        text = currentMedia.fileExtension.toUpperCase(Locale.current),
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                if (currentMedia.isEncrypted) {
                                    MediaInfoChip(
                                        text = stringResource(R.string.encrypted),
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                }
                                if (currentMedia.isCloud) {
                                    MediaInfoChip(
                                        text = stringResource(R.string.cloud_media),
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            LocationItem(
                                iconBackgroundModifier = Modifier
                                    .then(iconBackgroundModifier)
                                    .hazeEffectScaled(
                                        state = LocalHazeState.current,
                                        style = iconBackgroundHazeStyle
                                    ),
                                locationData = locationData,
                                mediaUri = currentMedia.getUri(),
                                onShowInApp = {
                                    allMetadataEventHandler.navigate(Screen.LocationsScreen())
                                }
                            )
                            AnimatedVisibility(visible = currentMedia.canMakeActions) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(buttonBackgroundModifier)
                                        .hazeEffectScaled(
                                            state = LocalHazeState.current,
                                            style = sheetCardButtonHazeStyle
                                        )
                                        .clickable(enabled = !metadataRemovalBusy) {
                                            probeMetadataSanitization(currentMedia)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocalFireDepartment,
                                        contentDescription = stringResource(R.string.remove_metadata),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.remove_metadata),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (motionPhotoState != null) {
                        item(key = "motion_photo") {
                            MotionPhotoShotsSection(
                                state = motionPhotoState,
                                modifier = Modifier
                                    .widthIn(max = 600.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(sheetCardBackgroundModifier)
                                    .hazeEffectScaled(
                                        state = LocalHazeState.current,
                                        style = sheetCardHazeStyle
                                    )
                                    .padding(16.dp)
                            )
                        }
                    }
                    if (cloudBackups.isNotEmpty()) {
                        item(key = "cloud_backup") {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 600.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(sheetCardBackgroundModifier)
                                    .hazeEffectScaled(
                                        state = LocalHazeState.current,
                                        style = sheetCardHazeStyle
                                    )
                                    .padding(vertical = 16.dp)
                            ) {
                                val backupProviders = remember(cloudBackups) {
                                    cloudBackups
                                        .mapNotNull { CloudUri.parse(it.uri.toString())?.providerType }
                                        .distinct()
                                }
                                MediaInfoRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    label = stringResource(R.string.cloud_backed_up_title),
                                    content = stringResource(R.string.cloud_backed_up_tap_hint),
                                    icon = Icons.Outlined.CloudDone,
                                    iconBackgroundModifier = Modifier
                                        .then(iconBackgroundModifier)
                                        .hazeEffectScaled(
                                            state = LocalHazeState.current,
                                            style = iconBackgroundHazeStyle
                                        ),
                                    trailingContent = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            backupProviders.forEach { providerType ->
                                                ProviderBrandIcon(
                                                    providerType = providerType,
                                                    modifier = Modifier.size(22.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        scope.launch { backupSheetState.show() }
                                    }
                                )
                            }
                        }
                    }
                    item(key = "media_info") {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .then(sheetCardBackgroundModifier)
                                .hazeEffectScaled(
                                    state = LocalHazeState.current,
                                    style = sheetCardHazeStyle
                                )
                                .padding(vertical = 16.dp)
                        ) {
                            mediaInfoList.forEach {
                                MediaInfoRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    label = it.label,
                                    content = it.content,
                                    icon = it.icon,
                                    iconBackgroundModifier = Modifier
                                        .then(iconBackgroundModifier)
                                        .hazeEffectScaled(
                                            state = LocalHazeState.current,
                                            style = iconBackgroundHazeStyle
                                        ),
                                    trailingContent = {
                                        if (it.trailingIcon != null && currentMedia.canMakeActions) {
                                            MediaInfoChip(
                                                text = stringResource(R.string.edit),
                                                contentColor = MaterialTheme.colorScheme.secondary,
                                                containerColor = MaterialTheme.colorScheme.secondary.copy(
                                                    alpha = 0.1f
                                                ),
                                                onClick = {
                                                    scope.launch {
                                                        metadataSheetState.show()
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    onClick = it.onClick
                                )
                            }
                            // Only offer "view all metadata" when there is metadata to show.
                            // metadata is null when nothing could be parsed for this item, so
                            // hiding the row avoids opening an empty metadata screen.
                            if (!currentMedia.isEncrypted && metadata != null) {
                                MediaInfoRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    label = stringResource(R.string.view_all_metadata),
                                    content = stringResource(R.string.metadata),
                                    icon = Icons.Outlined.Info,
                                    iconBackgroundModifier = Modifier
                                        .then(iconBackgroundModifier)
                                        .hazeEffectScaled(
                                            state = LocalHazeState.current,
                                            style = iconBackgroundHazeStyle
                                        ),
                                    onClick = {
                                        allMetadataEventHandler.navigate(
                                            Screen.MetadataViewScreen.uriAndType(
                                                mediaUri = currentMedia.getUri().toString(),
                                                isVideo = currentMedia.isVideo
                                            )
                                        )
                                    }
                                )
                            }
                            if (category != null) {
                                val eventHandler = LocalEventHandler.current
                                MediaInfoRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    label = category!!,
                                    content = stringResource(
                                        R.string.s_items,
                                        mediaCategoryCounter
                                    ),
                                    iconBackgroundModifier = Modifier
                                        .then(iconBackgroundModifier)
                                        .hazeEffectScaled(
                                            state = LocalHazeState.current,
                                            style = iconBackgroundHazeStyle
                                        ),
                                    trailingContent = {
                                        AnimatedVisibility(
                                            visible = mediaCategoryThumbnail != null,
                                            enter = enterAnimation,
                                            exit = exitAnimation
                                        ) {
                                            GlideImage(
                                                model = mediaCategoryThumbnail!!.uri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(16.dp)),
                                                requestBuilderTransform = {
                                                    it.signature(GlideInvalidation.signature(mediaCategoryThumbnail!!))
                                                }
                                            )
                                        }
                                    },
                                    onClick = {
                                        eventHandler.navigate(
                                            Screen.CategoryViewScreen.category(
                                                category!!
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                    item(key = "actions") {
                        MediaViewSheetActions(
                            media = currentMedia,
                            albumsState = albumsState,
                            vaults = vaultState,
                            restoreMedia = restoreMedia,
                            currentVault = currentVault
                        )
                    }
                    item(key = "spacer") {
                        NavigationBarSpacer()
                    }
                }

                if (metadataRemovalSheetState.isVisible) {
                    MetadataRemovalSheet(
                        state = metadataRemovalSheetState,
                        capability = sanitizationCapability,
                        isBusy = metadataRemovalBusy,
                        onConfirm = { mode ->
                            pendingRemovalMode = mode
                            scope.launch {
                                metadataRemovalPermissionResult.launchWriteRequest(
                                    currentMedia.writeRequest(context.contentResolver),
                                    doMetadataRemoval
                                )
                            }
                        }
                    )
                }

                if (metadataSheetState.isVisible) {
                    MetadataEditSheet(
                        state = metadataSheetState,
                        media = currentMedia,
                        metadata = metadata
                    )
                }

                if (cloudBackups.isNotEmpty()) {
                    CloudBackupInfoSheet(
                        sheetState = backupSheetState,
                        backups = cloudBackups
                    )
                }

            }
        }
    }
}
