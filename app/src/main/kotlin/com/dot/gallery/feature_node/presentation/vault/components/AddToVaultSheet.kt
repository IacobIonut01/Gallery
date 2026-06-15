package com.dot.gallery.feature_node.presentation.vault.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Settings
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToVaultSheet(
    state: AppBottomSheetState,
    onEncryptAndDelete: () -> Unit,
    onEncryptAndKeep: () -> Unit,
    onBehaviorChanged: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rememberChoice by remember { mutableStateOf(false) }

    if (state.isVisible) {
        ModalBottomSheet(
            sheetState = state.sheetState,
            onDismissRequest = {
                scope.launch { state.hide() }
                rememberChoice = false
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
            dragHandle = { DragHandle() },
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontStyle = MaterialTheme.typography.titleLarge.fontStyle,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing
                            )
                        ) {
                            append(stringResource(R.string.vault_add_to_vault_title))
                        }
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    text = stringResource(R.string.vault_originals_question),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Text(
                        text = stringResource(R.string.vault_remember_choice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SetupButton(
                        onClick = {
                            if (rememberChoice) {
                                onBehaviorChanged(Settings.Vault.ENCRYPT_KEEP)
                            }
                            onEncryptAndKeep()
                            scope.launch { state.hide() }
                            rememberChoice = false
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.vault_encrypt_and_keep)
                    )
                    SetupButton(
                        onClick = {
                            if (rememberChoice) {
                                onBehaviorChanged(Settings.Vault.ENCRYPT_DELETE)
                            }
                            onEncryptAndDelete()
                            scope.launch { state.hide() }
                            rememberChoice = false
                        },
                        applyHorizontalPadding = false,
                        applyBottomPadding = false,
                        applyInsets = false,
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.vault_encrypt_and_delete)
                    )
                }
            }
        }
    }
}
