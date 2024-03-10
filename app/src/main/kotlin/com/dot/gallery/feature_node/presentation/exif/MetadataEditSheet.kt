package com.dot.gallery.feature_node.presentation.exif

import android.media.MediaScannerConnection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.DragHandle
import com.dot.gallery.feature_node.domain.model.ExifAttributes
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.rememberExifAttributes
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.util.AppBottomSheetState
import com.dot.gallery.feature_node.presentation.util.rememberActivityResult
import com.dot.gallery.feature_node.presentation.util.rememberExifInterface
import com.dot.gallery.feature_node.presentation.util.rememberExifMetadata
import com.dot.gallery.feature_node.presentation.util.toastError
import com.dot.gallery.feature_node.presentation.util.writeRequest
import com.dot.gallery.ui.theme.Shapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditSheet(
    state: AppBottomSheetState,
    media: Media,
    handle: MediaHandleUseCase
) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    var shouldRemoveMetadata by remember { mutableStateOf(false) }
    var shouldRemoveLocation by remember { mutableStateOf(false) }
    val exifInterface = rememberExifInterface(media, useDirectPath = true)
    val context = LocalContext.current
    val cr = remember(context) { context.contentResolver }
    var exifAttributes by rememberExifAttributes(exifInterface)
    var newLabel by remember { mutableStateOf(media.label) }
    val errorToast = toastError()
    val request = rememberActivityResult {
        scope.launch {
            var done: Boolean
            done = handle.updateMediaExif(media, exifAttributes)
            if (newLabel != media.label && newLabel.isNotBlank()) {
                done = handle.renameMedia(media, newLabel)
            }
            if (done) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(media.path),
                    arrayOf(media.mimeType),
                    null
                )
                state.hide()
            } else {
                errorToast.show()
            }
        }
    }

    if (state.isVisible) {
        ModalBottomSheet(
            sheetState = state.sheetState,
            onDismissRequest = {
                scope.launch {
                    state.hide()
                }
            },
            dragHandle = { DragHandle() },
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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
                            append(stringResource(R.string.edit_metadata))
                        }
                        append("\n")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                letterSpacing = MaterialTheme.typography.bodyMedium.letterSpacing
                            )
                        ) {
                            append(stringResource(R.string.beta))
                        }
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(state = rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = shouldRemoveMetadata,
                        onClick = {
                            shouldRemoveLocation = false
                            shouldRemoveMetadata = !shouldRemoveMetadata
                        },
                        label = {
                            Text(text = stringResource(R.string.remove_metadata))
                        },
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(100),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                    val exifMetadata = exifInterface?.let { rememberExifMetadata(media, it) }
                    AnimatedVisibility(visible = exifMetadata?.gpsLatLong != null) {
                        FilterChip(
                            selected = shouldRemoveLocation,
                            onClick = {
                                shouldRemoveMetadata = false
                                shouldRemoveLocation = !shouldRemoveLocation
                            },
                            label = {
                                Text(text = stringResource(R.string.remove_location))
                            },
                            leadingIcon = {
                                Icon(
                                    modifier = Modifier.size(18.dp),
                                    imageVector = Icons.Outlined.GpsOff,
                                    contentDescription = null
                                )
                            },
                            enabled = exifMetadata?.gpsLatLong != null,
                            shape = RoundedCornerShape(100),
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                TextField(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    value = newLabel,
                    onValueChange = { newValue ->
                        newLabel = newValue
                    },
                    label = {
                        Text(text = stringResource(id = R.string.label))
                    },
                    singleLine = true,
                    shape = Shapes.large,
                    colors = TextFieldDefaults.colors(
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )

                TextField(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(112.dp),
                    value = exifAttributes.imageDescription ?: "",
                    onValueChange = { newValue ->
                        exifAttributes = exifAttributes.copy(imageDescription = newValue)
                    },
                    label = {
                        Text(text = stringResource(R.string.description))
                    },
                    shape = Shapes.large,
                    colors = TextFieldDefaults.colors(
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement
                        .spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
                    val tertiaryOnContainer = MaterialTheme.colorScheme.onTertiaryContainer
                    Button(
                        onClick = {
                            scope.launch { state.hide() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tertiaryContainer,
                            contentColor = tertiaryOnContainer
                        )
                    ) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.Main) {
                                if (shouldRemoveMetadata) {
                                    exifAttributes = ExifAttributes()
                                } else if (shouldRemoveLocation) {
                                    exifAttributes = exifAttributes.copy(gpsLatLong = null)
                                }
                                request.launch(media.writeRequest(cr))
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.action_confirm))
                    }
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}