/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.setup.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.R
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.ui.CloudAccountsViewModel
import com.dot.gallery.cloud.ui.descriptor.ProviderUiDescriptors
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.navigate
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.presentation.setup.components.SetupSectionCard
import com.dot.gallery.feature_node.presentation.setup.components.SetupWizardScaffold
import com.dot.gallery.feature_node.presentation.util.Screen

@Composable
fun SetupCloudPage(
    stepNumber: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val handler = LocalEventHandler.current
    val viewModel = hiltViewModel<CloudAccountsViewModel>()
    val configs by viewModel.accountState.collectAsStateWithLifecycle()
    val hasConfigured = configs.isNotEmpty()

    val providerTypes = remember { ProviderType.availableRemoteTypes() }

    SetupWizardScaffold(
        showBack = true,
        onBack = onBack,
        stepNumber = stepNumber,
        totalSteps = totalSteps,
        title = stringResource(R.string.setup_cloud_title),
        subtitle = stringResource(R.string.setup_cloud_subtitle),
        bottomBar = {
            // Cloud sync is optional: until an account is connected the only meaningful
            // action is to skip, so a single state-aware button is shown rather than a
            // redundant Continue + Skip pair.
            if (hasConfigured) {
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
                    text = stringResource(R.string.setup_skip_for_now),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    applyNavigationPadding = false,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onSkip
                )
            }
        }
    ) {
        var showProviderPicker by rememberSaveable { mutableStateOf(false) }
        // Show the provider chooser when nothing is configured yet, or when the user has
        // explicitly asked to add another provider on top of the existing one(s).
        val showChooser = !hasConfigured || showProviderPicker

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasConfigured) {
                configs.forEach { config ->
                    SetupSectionCard(
                        title = config.providerType.displayName,
                        subtitle = stringResource(R.string.setup_cloud_configured, config.serverUrl)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = stringResource(R.string.setup_cap_sync),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (showChooser) {
                Text(
                    text = stringResource(R.string.setup_cloud_choose_provider),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                providerTypes.forEach { type ->
                    val caps = remember(type) {
                        val live = viewModel.capabilitiesOf(type)
                        if (live.isNotEmpty()) live else defaultCapabilitiesFor(type)
                    }
                    ProviderCard(
                        type = type,
                        capabilities = caps,
                        onSetup = {
                            handler.navigate(Screen.CloudAddServerScreen.providerType(type.name))
                        }
                    )
                }
                if (providerTypes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.setup_cloud_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // At least one provider is connected — offer to add another.
                SetupButton(
                    text = stringResource(R.string.setup_cloud_add_another),
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    applyNavigationPadding = false,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { showProviderPicker = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderCard(
    type: ProviderType,
    capabilities: Set<ProviderCapability>,
    onSetup: () -> Unit
) {
    val descriptor = remember(type) { ProviderUiDescriptors.forType(type) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
            .clickable(onClick = onSetup)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = descriptor.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                text = type.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            capabilities.forEach { capability ->
                CapabilityChip(capability)
            }
        }
    }
}

@Composable
private fun CapabilityChip(capability: ProviderCapability) {
    val labelRes = capabilityLabel(capability) ?: return
    val label = stringResource(labelRes)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun capabilityLabel(capability: ProviderCapability): Int? = when (capability) {
    ProviderCapability.REMOTE_ASSETS -> R.string.setup_cap_remote_assets
    ProviderCapability.REMOTE_ALBUMS -> R.string.setup_cap_remote_albums
    ProviderCapability.SYNC -> R.string.setup_cap_sync
    ProviderCapability.PEOPLE -> R.string.setup_cap_people
    ProviderCapability.MAP -> R.string.setup_cap_map
    ProviderCapability.SMART_SEARCH -> R.string.setup_cap_smart_search
    ProviderCapability.TEXT_SEARCH -> R.string.setup_cap_text_search
    ProviderCapability.SHARE_CREATE -> R.string.setup_cap_share_link
    ProviderCapability.SHARE_MANAGE -> null
    ProviderCapability.OCR -> R.string.setup_cap_ocr
    ProviderCapability.ARCHIVE -> R.string.setup_cap_archive
    ProviderCapability.MEMORIES -> R.string.setup_cap_memories
    // Selection-only capabilities, not shown during setup.
    ProviderCapability.FAVORITE -> null
    ProviderCapability.TRASH -> null
}

private fun defaultCapabilitiesFor(type: ProviderType): Set<ProviderCapability> = when (type) {
    ProviderType.IMMICH -> setOf(
        ProviderCapability.REMOTE_ASSETS,
        ProviderCapability.REMOTE_ALBUMS,
        ProviderCapability.SYNC,
        ProviderCapability.PEOPLE,
        ProviderCapability.MAP,
        ProviderCapability.SMART_SEARCH,
        ProviderCapability.SHARE_CREATE,
        ProviderCapability.SHARE_MANAGE,
        ProviderCapability.ARCHIVE,
        ProviderCapability.MEMORIES
    )
    ProviderType.OWNCLOUD, ProviderType.NEXTCLOUD, ProviderType.WEBDAV -> setOf(
        ProviderCapability.REMOTE_ASSETS,
        ProviderCapability.REMOTE_ALBUMS,
        ProviderCapability.SYNC,
        ProviderCapability.SHARE_CREATE
    )
    ProviderType.SMB, ProviderType.NFS -> setOf(
        ProviderCapability.REMOTE_ASSETS,
        ProviderCapability.REMOTE_ALBUMS,
        ProviderCapability.SYNC
    )
    else -> setOf(ProviderCapability.REMOTE_ASSETS)
}
