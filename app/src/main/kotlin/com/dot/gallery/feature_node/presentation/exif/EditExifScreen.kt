package com.dot.gallery.feature_node.presentation.exif

import android.media.MediaScannerConnection
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.ExifAttributes
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.rememberExifAttributes
import com.dot.gallery.feature_node.domain.use_case.MediaHandleUseCase
import com.dot.gallery.feature_node.presentation.util.launchWriteRequest
import com.dot.gallery.feature_node.presentation.util.rememberActivityResult
import com.dot.gallery.feature_node.presentation.util.rememberExifInterface
import com.dot.gallery.ui.theme.Shapes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun EditExifScreen(
    media: Media?,
    navigateUp: () -> Unit,
    handle: MediaHandleUseCase
) {
    if (media == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        val scope = rememberCoroutineScope()
        val exifInterface = rememberExifInterface(media)
        val context = LocalContext.current
        val cr = remember(context) { context.contentResolver }
        var exifAttributes by rememberExifAttributes(exifInterface)
        var newLabel by remember { mutableStateOf(media.label) }

        val renameLabelRequest = rememberActivityResult {
            scope.launch {
                handle.renameMedia(media, newLabel)
                MediaScannerConnection.scanFile(context, arrayOf(media.path), arrayOf(media.mimeType)
                ) { _, _ -> }
            }
        }
        val metadataRequest = rememberActivityResult {
            scope.launch {
                handle.updateMediaExif(media, exifAttributes)
                MediaScannerConnection.scanFile(context, arrayOf(media.path), arrayOf(media.mimeType)
                ) { _, _ -> }
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.edit_metadata)) },
                    navigationIcon = {
                        IconButton(onClick = navigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                            .zIndex(4.0f)
                    ) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                scope.launch {
                                    if (newLabel != media.label && newLabel.isNotBlank()) {
                                        media.launchWriteRequest(cr, renameLabelRequest)
                                    }
                                    media.launchWriteRequest(cr, metadataRequest)
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.apply))
                        }
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = it.calculateTopPadding(),
                        bottom = it.calculateBottomPadding()
                    )
                    .padding(horizontal = 16.dp)
                    .verticalScroll(state = rememberScrollState()),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlideImage(
                        model = media.uri,
                        contentDescription = null,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = Shapes.large
                            )
                            .clip(Shapes.large)
                            .height(120.dp)
                            .aspectRatio(0.75f),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = Shapes.large
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .alpha(0.4f)
                        ) {
                            Text(
                                text = stringResource(R.string.date_modified),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = media.fullDate
                            )
                        }
                    }
                }
                Text(
                    modifier = Modifier
                        .padding(vertical = 16.dp),
                    text = "Metadata",
                    style = MaterialTheme.typography.titleMedium
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp),
                        value = exifAttributes.imageDescription ?: "",
                        onValueChange = { newValue ->
                            exifAttributes = exifAttributes.copy(imageDescription = newValue)
                        },
                        label = {
                            Text(text = "Description")
                        },
                        shape = Shapes.large,
                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        )
                    )
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = exifAttributes.manufacturerName ?: "",
                        onValueChange = { newValue ->
                            exifAttributes = exifAttributes.copy(manufacturerName = newValue)
                        },
                        label = {
                            Text(text = "Manufacturer Name")
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
                        modifier = Modifier.fillMaxWidth(),
                        value = exifAttributes.modelName ?: "",
                        onValueChange = { newValue ->
                            exifAttributes = exifAttributes.copy(modelName = newValue)
                        },
                        label = {
                            Text(text = "Model Name")
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = exifAttributes.isoValue?.toString() ?: "",
                            onValueChange = { newValue ->
                                exifAttributes =
                                    exifAttributes.copy(isoValue = if (newValue.isEmpty()) null else newValue.toInt())
                            },
                            label = {
                                Text(text = "ISO")
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
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
                            modifier = Modifier.weight(1f),
                            value = exifAttributes.focalLength?.toString() ?: "",
                            onValueChange = { newValue ->
                                exifAttributes =
                                    exifAttributes.copy(focalLength = if (newValue.isEmpty()) null else newValue.toDouble())
                            },
                            label = {
                                Text(text = "Focal Length")
                            },
                            suffix = {
                                Text(text = "mm")
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                autoCorrect = false,
                            ),
                            singleLine = true,
                            shape = Shapes.large,
                            colors = TextFieldDefaults.colors(
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = exifAttributes.apertureValue?.toString() ?: "",
                        onValueChange = { newValue ->
                            exifAttributes =
                                exifAttributes.copy(apertureValue = if (newValue.isEmpty()) null else newValue.toDouble())
                        },
                        label = {
                            Text(text = "Aperture")
                        },
                        prefix = {
                            Text(text = "f/")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            autoCorrect = false,
                        ),
                        singleLine = true,
                        shape = Shapes.large,
                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = exifAttributes.gpsLatLong?.get(0)?.toString() ?: "",
                            onValueChange = { newValue ->
                                exifAttributes.gpsLatLong?.set(0, newValue.toDouble())
                                exifAttributes =
                                    exifAttributes.copy(gpsLatLong = exifAttributes.gpsLatLong)
                            },
                            label = {
                                Text(text = "Latitude")
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
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
                            modifier = Modifier.weight(1f),
                            value = exifAttributes.gpsLatLong?.get(1)?.toString() ?: "",
                            onValueChange = { newValue ->
                                exifAttributes.gpsLatLong?.set(1, newValue.toDouble())
                                exifAttributes =
                                    exifAttributes.copy(gpsLatLong = exifAttributes.gpsLatLong)
                            },
                            label = {
                                Text(text = "Longitude")
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                autoCorrect = false,
                            ),
                            singleLine = true,
                            shape = Shapes.large,
                            colors = TextFieldDefaults.colors(
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            Toast.makeText(context, "Metadata cleared", Toast.LENGTH_SHORT).show()
                            exifAttributes = ExifAttributes()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(text = "Clear All Metadata")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            Toast.makeText(context, "GPS cleared", Toast.LENGTH_SHORT).show()
                            exifAttributes = exifAttributes.copy(gpsLatLong = null)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(text = "Clear GPS only")
                    }
                }
            }
        }
    }
}