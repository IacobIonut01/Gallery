package com.dot.gallery.feature_node.presentation.mediaview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Position
import com.dot.gallery.core.metadata.MetadataRemovalMode
import com.dot.gallery.core.metadata.SanitizationCapability
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.presentation.settings.components.RadioSettingsItem
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataRemovalSheet(
    state: AppBottomSheetState,
    capability: SanitizationCapability?,
    isBusy: Boolean,
    onConfirm: (MetadataRemovalMode) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf(MetadataRemovalMode.LOCATION) }
    if (!state.isVisible) return

    val limitation = capability?.limitation
    val supported = capability?.supports(selectedMode) == true
    LaunchedEffect(capability) {
        if (capability != null && !capability.supports(selectedMode)) {
            selectedMode = MetadataRemovalMode.entries.firstOrNull(capability::supports) ?: selectedMode
        }
    }
    ModalBottomSheet(
        sheetState = state.sheetState,
        onDismissRequest = {
            if (!isBusy) scope.launch { state.hide() }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        dragHandle = { DragHandle() },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.remove_metadata),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.remove_metadata_warning),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                MetadataRemovalMode.entries.forEachIndexed { index, mode ->
                    val modeSupported = capability?.supports(mode) == true
                    val position = when (index) {
                        0 -> Position.Top
                        MetadataRemovalMode.entries.lastIndex -> Position.Bottom
                        else -> Position.Middle
                    }
                    RadioSettingsItem(
                        title = mode.title(),
                        summary = mode.description(),
                        selected = selectedMode == mode,
                        enabled = modeSupported && !isBusy,
                        screenPosition = position,
                        applyPaddings = false,
                        onClick = { selectedMode = mode },
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.metadata_preserved_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.metadata_preserved_description),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (!limitation.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = limitation,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupButton(
                    onClick = { scope.launch { state.hide() } },
                    enabled = !isBusy,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.action_cancel)
                )
                SetupButton(
                    onClick = { onConfirm(selectedMode) },
                    enabled = supported && !isBusy,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    applyHorizontalPadding = false,
                    applyBottomPadding = false,
                    applyInsets = false,
                    modifier = Modifier.weight(1f),
                    text = if (isBusy) {
                        stringResource(R.string.remove_metadata_running)
                    } else {
                        stringResource(R.string.remove_metadata_confirm)
                    }
                )
            }
        }
    }
}

@Composable
private fun MetadataRemovalMode.title(): String = when (this) {
    MetadataRemovalMode.LOCATION -> stringResource(R.string.metadata_mode_location)
    MetadataRemovalMode.PRIVACY -> stringResource(R.string.metadata_mode_privacy)
    MetadataRemovalMode.EVERYTHING -> stringResource(R.string.metadata_mode_everything)
}

@Composable
private fun MetadataRemovalMode.description(): String = when (this) {
    MetadataRemovalMode.LOCATION -> stringResource(R.string.metadata_mode_location_description)
    MetadataRemovalMode.PRIVACY -> stringResource(R.string.metadata_mode_privacy_description)
    MetadataRemovalMode.EVERYTHING -> stringResource(R.string.metadata_mode_everything_description)
}
