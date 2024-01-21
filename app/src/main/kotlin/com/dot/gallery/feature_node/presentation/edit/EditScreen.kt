package com.dot.gallery.feature_node.presentation.edit

import android.media.MediaScannerConnection
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.dot.gallery.ui.theme.GalleryTheme
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty
import kotlinx.coroutines.launch

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
    var cropRotation by remember { mutableFloatStateOf(0f) }
    val options = remember {
        listOf(
            EditOption(
                title = "Crop",
                id = CROP
            ),
            EditOption(
                title = "Adjust",
                id = ADJUST
            ),
            EditOption(
                title = "Filters",
                id = FILTERS
            ),
            EditOption(
                title = "Markup",
                id = MARKUP
            ),
            EditOption(
                title = "More",
                id = MORE
            ),
        ).toMutableStateList()
    }
    val selectedOption = remember {
        mutableStateOf(options.first())
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                EditOptions(
                    options = options,
                    selectedOption = selectedOption
                )
                EditBottomBar(
                    onCancel = onNavigateUp,
                    enabled = !saving,
                    onOverride = {
                        crop = true
                        asCopy = false
                    },
                    onSaveCopy = {
                        crop = true
                        asCopy = true
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(it),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                this@Column.AnimatedVisibility(
                    visible = image != null && selectedOption.value.id == CROP,
                    enter = enterAnimation,
                    exit = exitAnimation
                ) {
                    val ctx = LocalContext.current
                    ImageCropper(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageBitmap = image!!.asImageBitmap(),
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
                        cropRotation = cropRotation,
                        onCropStart = {
                            saving = true
                        },
                        onCropSuccess = { newImage ->
                            scope.launch {
                                viewModel.saveImage(newImage, asCopy = asCopy) { success ->
                                    crop = false
                                    saving = false
                                    if (success) {
                                        MediaScannerConnection.scanFile(
                                            ctx,
                                            arrayOf(mediaRef!!.path),
                                            arrayOf(mediaRef!!.mimeType),
                                            null
                                        )
                                        onNavigateUp()
                                    } else {
                                        Toast.makeText(ctx, "Save failed.", Toast.LENGTH_LONG).show()
                                    }
                                }
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
            AnimatedVisibility(
                visible = selectedOption.value.id == CROP,
                enter = enterAnimation,
                exit = exitAnimation
            ) {
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

                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GalleryTheme(
        darkTheme = true
    ) {
        EditScreen(hiltViewModel())
    }
}