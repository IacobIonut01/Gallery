package com.dot.gallery.feature_node.presentation.frameextract

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.NavigationBackButton
import com.dot.gallery.core.presentation.components.SetupButton
import com.dot.gallery.feature_node.presentation.frameextract.components.FrameExportFormatSheet
import com.dot.gallery.feature_node.presentation.frameextract.components.FrameFilmstrip
import com.dot.gallery.feature_node.presentation.frameextract.components.SelectedFramesTray
import com.dot.gallery.feature_node.presentation.util.rememberWindowInsetsController
import com.dot.gallery.feature_node.presentation.util.toggleSystemBars
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FramePickerScreen(
    state: FramePickerUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onStep: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onToggleSelection: () -> Unit,
    onJump: (FrameIdentity) -> Unit,
    onRemove: (FrameIdentity) -> Unit,
    onClear: () -> Unit,
    onFormat: (FrameExportFormat) -> Unit,
    onExport: (Boolean) -> Unit,
    onCancelExport: () -> Unit,
    onView: (android.net.Uri) -> Unit = {},
    onShare: (List<android.net.Uri>) -> Unit = {},
) {
    var showFormat by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var fullScreenPreview by remember { mutableStateOf(false) }
    val ready = state as? FramePickerUiState.Ready
    val windowInsetsController = rememberWindowInsetsController()
    val isWide = LocalConfiguration.current.screenWidthDp >= 600
    val selectedCount = ready?.selection?.size ?: 0
    val busy = state is FramePickerUiState.PreparingSource || state is FramePickerUiState.Exporting
    val handleBack = {
        if (busy) confirmCancel = true else onBack()
    }
    BackHandler {
        if (fullScreenPreview) fullScreenPreview = false else handleBack()
    }
    LaunchedEffect(fullScreenPreview) {
        windowInsetsController.toggleSystemBars(show = !fullScreenPreview)
    }
    DisposableEffect(Unit) {
        onDispose { windowInsetsController.toggleSystemBars(show = true) }
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = fullScreenPreview && ready != null,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "framePreviewTransition",
        ) { showPreview ->
            val visibilityScope = this@AnimatedContent
            val sharedPreviewModifier = with(this@SharedTransitionLayout) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState("frame_picker_preview"),
                    animatedVisibilityScope = visibilityScope,
                )
            }
            if (showPreview && ready != null) {
                FullScreenPreview(
                    state = ready,
                    previewModifier = sharedPreviewModifier,
                    onClose = { fullScreenPreview = false },
                    onJump = onJump,
                    onToggleSelection = onToggleSelection,
                )
            } else {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.frame_picker_title)) },
                navigationIcon = {
                    NavigationBackButton(forcedAction = handleBack)
                },
                actions = {
                    Surface(
                        modifier = Modifier.padding(end = 16.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "$selectedCount/${FrameSelectionReducer.MAX_SELECTION}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            when (state) {
                is FramePickerUiState.PartialSuccess,
                is FramePickerUiState.Success -> SetupButton(
                    text = stringResource(R.string.frame_picker_done),
                    onClick = onBack,
                )
                else -> Unit
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state) {
                is FramePickerUiState.PreparingSource -> Preparing(state)
                is FramePickerUiState.Ready -> ReadyContent(
                    state = state,
                    isWide = isWide,
                    previewModifier = sharedPreviewModifier,
                    onOpenPreview = { fullScreenPreview = true },
                    onSave = { showFormat = true },
                    onStep = onStep,
                    onTogglePlayback = onTogglePlayback,
                    onToggleSelection = onToggleSelection,
                    onJump = onJump,
                    onRemove = onRemove,
                    onClear = onClear,
                )
                is FramePickerUiState.Exporting -> Exporting(state)
                is FramePickerUiState.Failure -> Failure(state, onRetry)
                is FramePickerUiState.PartialSuccess -> ResultContent(
                    title = stringResource(R.string.frame_picker_partial_success, state.saved.size, state.failed),
                    saved = state.saved,
                    warnings = state.warnings,
                    onView = onView,
                    onShare = onShare,
                )
                is FramePickerUiState.Success -> ResultContent(
                    title = stringResource(R.string.frame_picker_success, state.savedUris.size),
                    saved = state.savedUris,
                    warnings = state.warnings,
                    onView = onView,
                    onShare = onShare,
                )
            }
        }
    }
            }
        }
    }

    if (showFormat && ready != null) {
        FrameExportFormatSheet(
            selected = ready.format,
            onSelect = onFormat,
            onSave = {
                showFormat = false
                onExport(ready.selection.isEmpty())
            },
            onDismiss = { showFormat = false },
        )
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.frame_picker_cancel_title)) },
            text = { Text(stringResource(R.string.frame_picker_cancel_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    if (state is FramePickerUiState.Exporting) onCancelExport()
                    onBack()
                }) { Text(stringResource(R.string.cancel)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.keep_editing)) }
            },
        )
    }
}

@Composable
private fun FullScreenPreview(
    state: FramePickerUiState.Ready,
    previewModifier: Modifier,
    onClose: () -> Unit,
    onJump: (FrameIdentity) -> Unit,
    onToggleSelection: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val current = state.currentFrame?.identity
    val selected = current != null && current in state.selection
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Preview(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .then(previewModifier),
            shape = RoundedCornerShape(0.dp),
            showTimestamp = false,
            allowImageOverflow = true,
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.58f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.close),
                tint = Color.White,
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FrameFilmstrip(
                    frames = state.filmstrip,
                    current = current,
                    selected = state.selection.toSet(),
                    preferredTimeUs = state.preferredTimeUs,
                    onFrameClick = onJump,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = current?.let(::frameLabel).orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleSelection()
                        },
                        enabled = current != null,
                    ) {
                        Icon(
                            imageVector = if (selected) Icons.Outlined.Delete else Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                if (selected) R.string.frame_picker_remove_frame else R.string.frame_picker_add_frame
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Preparing(state: FramePickerUiState.PreparingSource) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(progress = { (state.progress ?: 0) / 100f })
        Spacer(Modifier.height(16.dp))
        Text(
            text = state.progress?.let { stringResource(R.string.frame_picker_preparing_progress, it) }
                ?: stringResource(R.string.frame_picker_preparing),
            color = Color.White,
        )
    }
}

@Composable
private fun ReadyContent(
    state: FramePickerUiState.Ready,
    isWide: Boolean,
    previewModifier: Modifier,
    onOpenPreview: () -> Unit,
    onSave: () -> Unit,
    onStep: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onToggleSelection: () -> Unit,
    onJump: (FrameIdentity) -> Unit,
    onRemove: (FrameIdentity) -> Unit,
    onClear: () -> Unit,
) {
    if (isWide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Preview(
                state = state,
                onOpenPreview = onOpenPreview,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(previewModifier),
            )
            Controls(
                state = state,
                onStep = onStep,
                onTogglePlayback = onTogglePlayback,
                onToggleSelection = onToggleSelection,
                onJump = onJump,
                onRemove = onRemove,
                onClear = onClear,
                scrollControls = true,
                onSave = onSave,
                modifier = Modifier
                    .widthIn(min = 340.dp, max = 440.dp)
                    .fillMaxHeight(),
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Preview(
                state = state,
                onOpenPreview = onOpenPreview,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(previewModifier),
            )
            Controls(
                state = state,
                onStep = onStep,
                onTogglePlayback = onTogglePlayback,
                onToggleSelection = onToggleSelection,
                onJump = onJump,
                onRemove = onRemove,
                onClear = onClear,
                scrollControls = false,
                onSave = onSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Preview(
    state: FramePickerUiState.Ready,
    modifier: Modifier = Modifier,
    onOpenPreview: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    showTimestamp: Boolean = true,
    allowImageOverflow: Boolean = false,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Black,
        tonalElevation = 2.dp,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            state.currentFrame?.let { frame ->
                val imageRatio = frame.bitmap.width.toFloat() / frame.bitmap.height.coerceAtLeast(1)
                val availableRatio = maxWidth.value / maxHeight.value.coerceAtLeast(1f)
                val frameModifier = if (imageRatio >= availableRatio) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(imageRatio)
                } else {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(imageRatio)
                }
                Box(
                    modifier = frameModifier
                        .then(if (allowImageOverflow) Modifier else Modifier.clipToBounds())
                        .pointerInput(frame.identity) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale == 1f) Offset.Zero else offset + pan
                            }
                        }
                        .pointerInput(frame.identity) {
                            detectTapGestures(
                                onTap = { onOpenPreview?.invoke() },
                                onDoubleTap = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                            )
                        },
                ) {
                    Image(
                        bitmap = frame.bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.frame_picker_preview_description),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                    )
                    if (showTimestamp) {
                        Text(
                            text = frameLabel(frame.identity),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            if (state.isPreviewLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                )
            }
        }
    }
}

@Composable
private fun Controls(
    state: FramePickerUiState.Ready,
    onStep: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onToggleSelection: () -> Unit,
    onJump: (FrameIdentity) -> Unit,
    onRemove: (FrameIdentity) -> Unit,
    onClear: () -> Unit,
    scrollControls: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val current = state.currentFrame?.identity
    val scrollState = rememberScrollState()
    val selected = current != null && current in state.selection
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = if (scrollControls) {
                    Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                } else {
                    Modifier.fillMaxWidth()
                },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FrameFilmstrip(
                    frames = state.filmstrip,
                    current = current,
                    selected = state.selection.toSet(),
                    preferredTimeUs = state.preferredTimeUs,
                    onFrameClick = onJump,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = current?.let(::frameLabel).orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = formatTimeUs(state.metadata.durationUs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RepeatingStepButton(
                        icon = Icons.Outlined.SkipPrevious,
                        contentDescription = stringResource(R.string.frame_picker_previous_frame),
                        onStep = { onStep(-1) },
                    )
                    FilledIconButton(
                        onClick = onTogglePlayback,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            stringResource(if (state.isPlaying) R.string.frame_picker_pause else R.string.frame_picker_play),
                        )
                    }
                    RepeatingStepButton(
                        icon = Icons.Outlined.SkipNext,
                        contentDescription = stringResource(R.string.frame_picker_next_frame),
                        onStep = { onStep(1) },
                    )
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleSelection()
                        },
                        enabled = current != null,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(
                            if (selected) Icons.Outlined.Delete else Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                if (selected) R.string.frame_picker_remove_frame else R.string.frame_picker_add_frame
                            )
                        )
                    }
                }
                SelectedFramesTray(
                    frames = state.selection,
                    thumbnails = state.selectedThumbnails,
                    onJump = onJump,
                    onRemove = onRemove,
                    onClear = onClear,
                )
            }
            Spacer(Modifier.height(16.dp))
            SetupButton(
                text = saveButtonText(state),
                enabled = state.currentFrame != null,
                applyHorizontalPadding = false,
                applyBottomPadding = false,
                applyInsets = false,
                applyNavigationPadding = false,
                onClick = onSave,
            )
        }
    }
}

@Composable
private fun RepeatingStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onStep: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .pointerInput(onStep) {
                detectTapGestures(
                    onPress = {
                        onStep()
                        coroutineScope {
                            val repeatJob = launch {
                                delay(350)
                                while (isActive) {
                                    onStep()
                                    delay(120)
                                }
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                repeatJob.cancel()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Exporting(state: FramePickerUiState.Exporting) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(progress = { state.done.toFloat() / state.total.coerceAtLeast(1) })
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.frame_picker_export_progress, state.done, state.total),
            color = Color.White,
        )
        Text(state.phase, color = Color.LightGray)
    }
}

@Composable
private fun Failure(state: FramePickerUiState.Failure, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.reason, color = Color.White)
        if (state.retryable) TextButton(onClick = onRetry) { Text(stringResource(R.string.frame_picker_retry)) }
    }
}

@Composable
private fun ResultContent(
    title: String,
    saved: List<android.net.Uri>,
    warnings: Int,
    onView: (android.net.Uri) -> Unit,
    onShare: (List<android.net.Uri>) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = Color.White)
        if (warnings > 0) {
            Text(
                stringResource(R.string.frame_picker_metadata_warnings, warnings),
                color = Color.LightGray,
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            if (saved.size == 1) {
                TextButton(onClick = { onView(saved.first()) }) {
                    Text(stringResource(R.string.frame_picker_view))
                }
            }
            if (saved.isNotEmpty()) {
                TextButton(onClick = { onShare(saved) }) {
                    Text(stringResource(R.string.frame_picker_share))
                }
            }
        }
    }
}

@Composable
private fun saveButtonText(state: FramePickerUiState.Ready): String =
    if (state.selection.isEmpty()) {
        stringResource(R.string.frame_picker_save_current)
    } else {
        stringResource(R.string.frame_picker_save_count, state.selection.size)
    }

private fun formatTimeUs(timeUs: Long): String {
    val totalMs = timeUs.coerceAtLeast(0L) / 1000L
    val hours = totalMs / 3_600_000L
    val minutes = (totalMs / 60_000L) % 60L
    val seconds = (totalMs / 1000L) % 60L
    return if (hours > 0L) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

private fun frameLabel(identity: FrameIdentity): String {
    val totalMs = identity.presentationTimeUs / 1000L
    val minutes = totalMs / 60_000L
    val seconds = (totalMs / 1000L) % 60L
    val millis = totalMs % 1000L
    val time = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${millis.toString().padStart(3, '0')}"
    return if (identity.frameIndex >= 0) "$time  •  #${identity.frameIndex}" else time
}
