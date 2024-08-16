package com.dot.gallery.feature_node.presentation.edit

import android.media.MediaScannerConnection
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.dot.gallery.feature_node.presentation.edit.components.HorizontalScrubber
import com.dot.gallery.feature_node.presentation.edit.components.adjustments.AdjustmentFilter
import com.dot.gallery.feature_node.presentation.edit.components.adjustments.AdjustmentSelector
import com.dot.gallery.feature_node.presentation.edit.components.crop.CropOptions
import com.dot.gallery.feature_node.presentation.edit.components.crop.Cropper
import com.dot.gallery.feature_node.presentation.edit.components.filters.FilterSelector
import com.dot.gallery.feature_node.presentation.edit.components.more.MoreSelector
import com.dot.gallery.feature_node.presentation.util.getEditImageCapableApps
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditScreen(
    viewModel: EditViewModel,
    onNavigateUp: () -> Unit = {},
) {
    val image by viewModel.image.collectAsStateWithLifecycle()
    val mediaRef by viewModel.mediaRef.collectAsStateWithLifecycle()
    var crop by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val editApps = remember(context, context::getEditImageCapableApps)
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
    val selectedAdjFilter = remember {
        mutableStateOf<Pair<AdjustmentFilter, Float>?>(null)
    }
    val scope = rememberCoroutineScope(getContext = { Dispatchers.IO })
    val filters by viewModel.filters.collectAsStateWithLifecycle(context = Dispatchers.IO)
    val cropEnabled by remember(selectedOption.value) { mutableStateOf(selectedOption.value.id == CROP) }
    val pagerState = rememberPagerState { options.size }
    LaunchedEffect(selectedOption.value.id) {
        pagerState.scrollToPage(
            when (selectedOption.value.id) {
                CROP -> 0
                ADJUST -> 1
                FILTERS -> 2
                MARKUP -> 3
                MORE -> 4
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
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
                        scope.launch {
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
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .animateContentSize()
                .padding(paddingValues),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
            ) {
                image?.let { imageBitmap ->
                    Cropper(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        bitmap = imageBitmap,
                        cropEnabled = cropEnabled,
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
                            pannable = true,
                            fling = false,
                            rotatable = false
                        ),
                        crop = crop,
                        onCropStart = {
                            saving = true
                        },
                        onCropSuccess = { newImage ->
                            scope.launch {
                                viewModel.addCroppedImage(newImage)
                            }
                            crop = false
                            saving = false
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

            HorizontalPager(
                modifier = Modifier
                    .wrapContentHeight()
                    .animateContentSize(),
                userScrollEnabled = false,
                beyondViewportPageCount = 1,
                verticalAlignment = Alignment.Bottom,
                state = pagerState
            ) { page ->
                when (page) {
                    0 -> {
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

                    1 -> {
                        Column {
                            val state =
                                rememberPagerState(pageCount = EditViewModel.adjustmentFilters::size)
                            LaunchedEffect(selectedAdjFilter.value) {
                                selectedAdjFilter.value?.let {
                                    val index = EditViewModel.adjustmentFilters.indexOf(it.first)
                                    state.scrollToPage(index)
                                }
                            }
                            AnimatedVisibility(
                                visible = selectedAdjFilter.value != null,
                                enter = slideInVertically(),
                                exit = slideOutVertically()
                            ) {
                                HorizontalPager(
                                    state = state,
                                    userScrollEnabled = false,
                                ) {
                                    val adjustment = EditViewModel.adjustmentFilters[it]
                                    var currentValue by rememberSaveable {
                                        mutableFloatStateOf(
                                            selectedAdjFilter.value?.second
                                                ?: adjustment.defaultValue
                                        )
                                    }
                                    HorizontalScrubber(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        displayValue = { value ->
                                            (value * 100).roundToInt().toString()
                                        },
                                        minValue = remember(adjustment, adjustment::minValue),
                                        maxValue = remember(adjustment, adjustment::maxValue),
                                        defaultValue = remember(
                                            adjustment,
                                            adjustment::defaultValue
                                        ),
                                        allowNegative = remember(adjustment) { adjustment.minValue < 0f },
                                        currentValue = currentValue,
                                        onValueChanged = { isScrolling, newValue ->
                                            scope.launch {
                                                if (selectedAdjFilter.value != null) {
                                                    viewModel.addAdjustment(
                                                        isScrolling,
                                                        selectedAdjFilter.value!!.first to newValue
                                                    )
                                                    currentValue = newValue
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            AdjustmentSelector(
                                viewModel = viewModel,
                                selectedFilter = selectedAdjFilter
                            )
                        }
                    }

                    2 -> {
                        FilterSelector(
                            filters = filters,
                            viewModel = viewModel
                        )
                    }

                    3 -> {
                        // TODO
                    }

                    4 -> {
                        MoreSelector(
                            editApps = editApps,
                            currentUri = viewModel.currentUri
                        )
                    }
                }
            }
        }
    }
}

