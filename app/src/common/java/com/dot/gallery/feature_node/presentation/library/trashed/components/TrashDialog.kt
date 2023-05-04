package com.dot.gallery.feature_node.presentation.library.trashed.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.ui.core.icons.Face
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashDialog(
    openBottomSheet: MutableState<Boolean>,
    sheetState: SheetState,
    data: List<Media>,
    defaultText: @Composable (size: Int) -> String = {
        stringResource(R.string.delete_dialog_title, it)
    },
    confirmedText: @Composable (size: Int) -> String = {
        stringResource(R.string.delete_dialog_title_confirmation, it)
    },
    image: ImageVector = Icons.Outlined.DeleteOutline,
    onConfirm: suspend (List<Media>) -> Unit
) {
    val scope = rememberCoroutineScope()
    if (openBottomSheet.value) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                scope.launch {
                    openBottomSheet.value = false
                    sheetState.hide()
                }
            },
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 11.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(Modifier.size(width = 32.dp, height = 4.dp))
                }
            }
        ) {
            val defaultText2 = defaultText.invoke(data.size)
            val confirmedText2 = confirmedText.invoke(data.size)
            var confirmed by remember { mutableStateOf(false) }
            val text = remember(confirmed) {
                if (confirmed) confirmedText2 else defaultText2
            }
            val primaryContainer = MaterialTheme.colorScheme.primaryContainer
            val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
            val containerBackground by animateColorAsState(
                targetValue = if (confirmed) tertiaryContainer else primaryContainer
            )
            val primaryOnContainer = MaterialTheme.colorScheme.onPrimaryContainer
            val tertiaryOnContainer = MaterialTheme.colorScheme.onTertiaryContainer
            val onContainerBackground by animateColorAsState(
                targetValue = if (confirmed) tertiaryOnContainer else primaryOnContainer
            )
            val mainButtonDefaultText = stringResource(R.string.action_confirm)
            val mainButtonConfirmText = stringResource(R.string.action_confirmed)
            val mainButtonText = remember(confirmed) {
                if (confirmed) mainButtonConfirmText else mainButtonDefaultText
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                ) {
                    Image(
                        imageVector = com.dot.gallery.ui.core.Icons.Face,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(containerBackground),
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        imageVector = image,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(onContainerBackground),
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center)
                    )
                }
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement
                        .spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    AnimatedVisibility(visible = !confirmed) {
                        Button(
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                    openBottomSheet.value = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tertiaryContainer,
                                contentColor = tertiaryOnContainer
                            )
                        ) {
                            Text(text = stringResource(R.string.action_cancel))
                        }
                    }
                    Button(
                        enabled = !confirmed,
                        onClick = {
                            confirmed = true
                            scope.launch {
                                onConfirm.invoke(data)
                            }
                        }
                    ) {
                        Text(text = mainButtonText)
                    }
                }
            }
        }
    }
}

