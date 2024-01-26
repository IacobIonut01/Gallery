package com.dot.gallery.feature_node.presentation.edit

import android.media.MediaScannerConnection
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.feature_node.presentation.edit.components.EditBottomBar
import com.dot.gallery.feature_node.presentation.edit.components.EditId.ADJUST
import com.dot.gallery.feature_node.presentation.edit.components.EditId.CROP
import com.dot.gallery.feature_node.presentation.edit.components.EditId.FILTERS
import com.dot.gallery.feature_node.presentation.edit.components.EditId.MARKUP
import com.dot.gallery.feature_node.presentation.edit.components.EditId.MORE
import com.dot.gallery.feature_node.presentation.edit.components.EditOption
import com.dot.gallery.feature_node.presentation.edit.components.EditOptions
import com.dot.gallery.feature_node.presentation.edit.components.crop.CropOptions
import com.dot.gallery.feature_node.presentation.edit.components.crop.Cropper
import com.dot.gallery.feature_node.presentation.edit.components.filters.FilterSelector
import com.dot.gallery.feature_node.presentation.util.getEditImageCapableApps
import com.dot.gallery.feature_node.presentation.util.launchEditImageIntent
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty
import kotlin.math.roundToInt

@Composable
fun EditScreen(
    viewModel: EditViewModel,
    onNavigateUp: () -> Unit = {},
) {
    val image by viewModel.image.collectAsStateWithLifecycle()
    val mediaRef by viewModel.mediaRef.collectAsStateWithLifecycle()
    var crop by remember { mutableStateOf(false) }
    var asCopy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val editApps = remember(context) {
        context.getEditImageCapableApps()
    }
    val options = remember(editApps) {
        mutableStateListOf(
            EditOption(
                title = "Crop",
                id = CROP
            ),
            EditOption(
                title = "Adjust",
                isEnabled = false,
                id = ADJUST
            ),
            EditOption(
                title = "Filters",
                id = FILTERS
            ),
            EditOption(
                title = "Markup",
                isEnabled = false,
                id = MARKUP
            )
        )
    }
    LaunchedEffect(editApps) {
        if (editApps.isNotEmpty()) {
            options.add(
                EditOption(
                    title = "More",
                    id = MORE
                )
            )
        }
    }
    val selectedOption = remember {
        mutableStateOf(options.first())
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = selectedOption.value.id == FILTERS,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    FilterSelector(viewModel)
                }
                AnimatedVisibility(
                    visible = selectedOption.value.id == MORE,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        items(editApps) { app ->
                            val icon = remember(app) {
                                try {
                                    app.loadIcon(context.packageManager).toBitmap().asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            icon?.let {
                                EditApp(
                                    bitmap = it,
                                    title = app.loadLabel(context.packageManager).toString()
                                ) {
                                    context.launchEditImageIntent(
                                        app.activityInfo.packageName,
                                        viewModel.currentUri
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                EditOptions(
                    options = options,
                    selectedOption = selectedOption
                )
                EditBottomBar(
                    onCancel = onNavigateUp,
                    enabled = !saving,
                    canRevert = viewModel.canRevert.value,
                    onOverride = { },
                    onRevert = viewModel::revert,
                    onSaveCopy = {
                        image?.let {
                            viewModel.saveImage(it.asImageBitmap(), asCopy = true) { success ->
                                if (success) {
                                    MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(mediaRef!!.path),
                                        arrayOf(mediaRef!!.mimeType),
                                        null
                                    )
                                    onNavigateUp()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Save failed.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .animateContentSize()
                .padding(it),
        ) {
            val animHeight = with(LocalDensity.current) { 64.dp.toPx().roundToInt() }

            AnimatedVisibility(
                modifier = Modifier.animateContentSize(),
                visible = selectedOption.value.id != CROP,
                enter = slideInVertically { -animHeight } + fadeIn(),
                exit = slideOutVertically { -animHeight / 2 } + fadeOut(tween(1000))
            ) {
                image?.asImageBitmap()?.let { image ->
                    Image(
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize()
                            .padding(horizontal = 16.dp),
                        bitmap = image,
                        contentDescription = "Edited image"
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedOption.value.id == CROP,
                enter = fadeIn(tween(1000)),
                exit = slideOutVertically { animHeight } + fadeOut()
            ) {
                Column {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        image?.let { imageBitmap ->
                            Cropper(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                bitmap = imageBitmap,
                                cropProperties = CropDefaults.properties(
                                    cropOutlineProperty = CropOutlineProperty(
                                        outlineType = OutlineType.RoundedRect,
                                        cropOutline = RectCropShape(
                                            id = 0,
                                            title = OutlineType.RoundedRect.name
                                        )
                                    ),
                                    maxZoom = 5f,
                                    overlayRatio = 1f,
                                    fling = false,
                                    rotatable = false
                                ),
                                crop = crop,
                                onCropStart = {
                                    saving = true
                                },
                                onCropSuccess = { newImage ->
                                    viewModel.updateImage(newImage) {
                                        crop = false
                                        saving = false
                                    }
                                }
                            )
                        }

                        this@Column.AnimatedVisibility(
                            visible = saving,
                            enter = enterAnimation,
                            exit = exitAnimation
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color = Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        /*CropScrubber(
                            onAngleChanged = { newAngle ->
                                //cropRotation = newAngle
                                viewModel.setAngle(newAngle)
                            }
                        )*/
                        CropOptions(
                            onMirrorPressed = {
                                viewModel.flipHorizontally()
                            },
                            onRotatePressed = {
                                //cropRotation += 90f
                                viewModel.addAngle(90f)
                            },
                            onAspectRationPressed = {

                            },
                            onCropPressed = {
                                crop = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditApp(
    bitmap: ImageBitmap,
    title: String,
    onItemClick: () -> Unit
) {
    val tintColor = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .defaultMinSize(minHeight = 80.dp)
            .clickable(onClick = onItemClick)
            .padding(top = 12.dp, bottom = 16.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = title,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = title,
            modifier = Modifier,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = tintColor,
            textAlign = TextAlign.Center
        )
    }
}