/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.dot.gallery.core.Settings
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.image.CloudImageSource
import com.dot.gallery.core.decoder.EncryptedRegionDecoder
import com.dot.gallery.core.decoder.FullImageRegionDecoder
import com.dot.gallery.core.decoder.JxlRegionDecoder
import com.dot.gallery.core.presentation.components.util.LocalBatteryStatus
import com.dot.gallery.core.presentation.components.util.ProvideBatteryStatus
import com.dot.gallery.core.presentation.components.util.swipe
import com.dot.gallery.feature_node.data.data_source.KeychainHolder
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.util.asSubsamplingImage
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.domain.util.isApng
import com.dot.gallery.feature_node.domain.util.isAvif
import com.dot.gallery.feature_node.domain.util.isCloud
import com.dot.gallery.feature_node.domain.util.isEncrypted
import com.dot.gallery.feature_node.domain.util.isJp2
import com.dot.gallery.feature_node.domain.util.isJxl
import com.dot.gallery.feature_node.domain.util.isPsd
import com.dot.gallery.feature_node.domain.util.isSvg
import com.dot.gallery.feature_node.domain.util.isTiff
import com.dot.gallery.feature_node.presentation.mediaview.rememberedDerivedState
import com.dot.gallery.feature_node.presentation.util.rememberFeedbackManager
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.PainterState
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.resize.Precision
import com.github.panpf.zoomimage.ZoomImage
import com.github.panpf.zoomimage.rememberSketchZoomState
import com.github.panpf.zoomimage.subsampling.SubsamplingImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import android.graphics.Bitmap
import androidx.compose.foundation.layout.width
import com.dot.gallery.core.ml.CutoutHelper
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun <T : Media> BlurredMediaBackground(
    media: T,
    uiEnabled: Boolean,
) {
    ProvideBatteryStatus {
        val allowBlur by Settings.Misc.rememberAllowBlur()
        val isPowerSavingMode = LocalBatteryStatus.current.isPowerSavingMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && allowBlur && !isPowerSavingMode) {
            val isEncrypted = remember(media) {
                media.isEncrypted
            }
            val blurAlpha by animateFloatAsState(
                animationSpec = tween(DEFAULT_TOP_BAR_ANIMATION_DURATION),
                targetValue = if (uiEnabled) 0.7f else 0f,
                label = "blurAlpha"
            )
            AsyncImage(
                request = ComposableImageRequest(media.getUri().toString()) {
                    resize(width = 600, height = 600, precision = Precision.LESS_PIXELS)
                    crossfade(false)
                    setExtra("realMimeType", media.mimeType)
                    if (isEncrypted) {
                        setExtra(key = "mediaKeyPreviewEnc", value = media.idLessKey)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(blurAlpha)
                    .blur(100.dp),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Stable
@Composable
fun <T : Media> ZoomablePagerImage(
    modifier: Modifier = Modifier,
    media: T,
    rotationDisabled: Boolean,
    onImageRotated: (newRotation: Int) -> Unit,
    onItemClick: () -> Unit,
    onSwipeDown: () -> Unit,
    onCutoutStateChanged: (Boolean) -> Unit = {},
    isSelected: Boolean = true
) {
    val feedbackManager = rememberFeedbackManager()
    var isRotating by rememberSaveable(media) { mutableStateOf(false) }
    var currentRotation by rememberSaveable(media) { mutableIntStateOf(0) }
    val rotationAnimation by animateFloatAsState(
        targetValue = if (isRotating) 90f else 0f,
        label = "rotationAnimation"
    )
    val zoomState = rememberSketchZoomState()
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val mediaUri = remember(media) {
        media.getUri().toString()
    }
    val isEncrypted = remember(media) {
        media.isEncrypted
    }
    val isJxl = remember(media) { media.isJxl }
    val isAnimated = remember(media) {
        media.isApng || media.isJxl || (media.isAvif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }
    // Region decoder for formats Android's BitmapRegionDecoder can't subsample (PSD/JP2/TIFF/SVG).
    // Without this they only show the screen-resolution base painter and look blurry when zoomed.
    val customRegionFactory = remember(media) {
        when {
            media.isPsd -> FullImageRegionDecoder.forPsd()
            media.isJp2 -> FullImageRegionDecoder.forJp2()
            media.isTiff -> FullImageRegionDecoder.forTiff()
            media.isSvg -> FullImageRegionDecoder.forSvg()
            else -> null
        }
    }

    // Fast low-res preview painter, shown until full image loads
    val previewPainter = rememberAsyncImagePainter(
        request = ComposableImageRequest(mediaUri) {
            resize(width = 600, height = 600, precision = Precision.LESS_PIXELS)
            crossfade(false)
            setExtra("realMimeType", media.mimeType)
            if (isEncrypted) {
                setExtra(key = "mediaKeyPreviewEnc", value = media.idLessKey)
            }
        },
        contentScale = ContentScale.Fit
    )

    // Full-res painter with state tracking
    val fullImageState = rememberAsyncImageState()
    val fullPainter = rememberAsyncImagePainter(
        request = ComposableImageRequest(mediaUri) {
            if (isEncrypted || isAnimated) {
                crossfade(durationMillis = 200)
            }
            setExtra("realMimeType", media.mimeType)
            if (isEncrypted) {
                setExtra(key = "mediaKeyPreviewEnc", value = media.idLessKey)
            }
        },
        state = fullImageState,
        contentScale = ContentScale.Fit
    )

    val isFullImageLoaded by rememberedDerivedState(media) {
        fullImageState.painterState is PainterState.Success
    }
    val activePainter = remember(isFullImageLoaded) {
        if (isFullImageLoaded) fullPainter else previewPainter
    }

    val isCloudMedia = remember(media) { media.isCloud }

    if (isEncrypted) {
        val keychainHolder = remember { KeychainHolder(context) }
        LaunchedEffect(media, isFullImageLoaded, zoomState.subsampling) {
            zoomState.setSubsamplingImage(media.asSubsamplingImage(context))
        }
        LaunchedEffect(zoomState.subsampling, media) {
            zoomState.subsampling.setRegionDecoders(
                listOf(
                    EncryptedRegionDecoder.Factory(
                        keychainHolder
                    )
                )
            )
        }
    } else if (isCloudMedia) {
        LaunchedEffect(media, isFullImageLoaded, zoomState.subsampling) {
            val uri = media.getUri()
            val providerName = uri.authority ?: return@LaunchedEffect
            val providerType = try { ProviderType.valueOf(providerName) } catch (_: Exception) { return@LaunchedEffect }
            val remoteId = uri.pathSegments?.firstOrNull() ?: return@LaunchedEffect
            val cloudSource = CloudImageSource(providerType, remoteId)
            zoomState.setSubsamplingImage(SubsamplingImage(imageSource = cloudSource))
        }
    } else if (isJxl) {
        // Android's BitmapRegionDecoder can't decode JXL, so enable subsampling backed by a
        // JxlCoder region decoder for high-resolution zoom. Animated JXL is rejected by the
        // decoder and falls back to the animated base painter.
        LaunchedEffect(media, isFullImageLoaded, zoomState.subsampling) {
            zoomState.setSubsamplingImage(media.asSubsamplingImage(context))
        }
        LaunchedEffect(zoomState.subsampling, media) {
            zoomState.subsampling.setRegionDecoders(listOf(JxlRegionDecoder.Factory()))
        }
    } else if (customRegionFactory != null) {
        // PSD/JP2/TIFF/SVG: no native BitmapRegionDecoder support, so subsample via a
        // full-decode-then-crop (PSD/JP2/TIFF) or high-res render (SVG) region decoder.
        LaunchedEffect(media, isFullImageLoaded, zoomState.subsampling) {
            zoomState.setSubsamplingImage(media.asSubsamplingImage(context))
        }
        LaunchedEffect(zoomState.subsampling, media) {
            zoomState.subsampling.setRegionDecoders(listOf(customRegionFactory))
        }
    } else if (!isAnimated) {
        LaunchedEffect(media, isFullImageLoaded, zoomState.subsampling) {
            zoomState.setSubsamplingImage(media.asSubsamplingImage(context))
        }
    }

    val modelManager = remember { (context.applicationContext as com.dot.gallery.GalleryApp).modelManager }
    var cutoutSession by remember { mutableStateOf<CutoutHelper.CutoutSession?>(null) }
    var promptPoints by remember { mutableStateOf<List<CutoutHelper.PromptPoint>>(emptyList()) }
    var promptPointsHistory by remember { mutableStateOf<List<List<CutoutHelper.PromptPoint>>>(emptyList()) }
    var historyIndex by remember { mutableIntStateOf(-1) }

    val updatePointsWithHistory = { newPoints: List<CutoutHelper.PromptPoint> ->
        val newHistory = promptPointsHistory.take(historyIndex + 1) + listOf(newPoints)
        promptPointsHistory = newHistory
        historyIndex = newHistory.size - 1
        promptPoints = newPoints
    }

    var activeTool by remember { mutableStateOf(ZoomablePagerImagePointTool.NONE) }
    var isRefining by remember { mutableStateOf(false) }

    var processingCutout by remember { mutableStateOf(false) }
    var cutoutResult by remember { mutableStateOf<CutoutHelper.CutoutResult?>(null) }
    var lastResultCache by remember { mutableStateOf<Pair<List<CutoutHelper.PromptPoint>, CutoutHelper.CutoutResult>?>(null) }

    val updateResultAndCache = { newResult: CutoutHelper.CutoutResult?, newCache: Pair<List<CutoutHelper.PromptPoint>, CutoutHelper.CutoutResult>? ->
        val bitmapsToKeep = listOfNotNull(newResult?.bitmap, newCache?.second?.bitmap)
        cutoutResult?.bitmap?.let { bmp ->
            if (bmp !in bitmapsToKeep) bmp.recycle()
        }
        lastResultCache?.second?.bitmap?.let { bmp ->
            if (bmp !in bitmapsToKeep) bmp.recycle()
        }
        cutoutResult = newResult
        lastResultCache = newCache
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowRadius"
    )

    DisposableEffect(media) {
        onDispose {
            cutoutSession?.close()
            cutoutSession = null
            promptPoints = emptyList()
            promptPointsHistory = emptyList()
            historyIndex = -1
            updateResultAndCache(null, null)
        }
    }

    BackHandler(enabled = cutoutSession != null) {
        cutoutSession?.close()
        cutoutSession = null
        promptPoints = emptyList()
        promptPointsHistory = emptyList()
        historyIndex = -1
        updateResultAndCache(null, null)
        activeTool = ZoomablePagerImagePointTool.NONE
    }

    LaunchedEffect(cutoutSession != null, isSelected) {
        if (isSelected) {
            onCutoutStateChanged(cutoutSession != null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ZoomImage(
            zoomState = zoomState,
            painter = activePainter,
            modifier = Modifier
                .fillMaxSize()
                .swipe(onSwipeDown = onSwipeDown)
                .graphicsLayer {
                    rotationZ = if (isRotating) rotationAnimation else 0f
                }
                .then(modifier),
            onTap = { offset ->
                val session = cutoutSession
                if (session != null) {
                    val displayRect = zoomState.zoomable.contentDisplayRect
                    val isInsideImage = offset.x >= displayRect.left &&
                            offset.x <= displayRect.right &&
                            offset.y >= displayRect.top &&
                            offset.y <= displayRect.bottom

                    if (isInsideImage && (activeTool == ZoomablePagerImagePointTool.ADD || activeTool == ZoomablePagerImagePointTool.REMOVE)) {
                        val contentPoint = zoomState.zoomable.touchPointToContentPoint(offset)
                        val contentSize = zoomState.zoomable.contentSize
                        val scaleX = if (contentSize.width > 0) session.widthOrig.toFloat() / contentSize.width.toFloat() else 1f
                        val scaleY = if (contentSize.height > 0) session.heightOrig.toFloat() / contentSize.height.toFloat() else 1f

                        val scaledX = contentPoint.x * scaleX
                        val scaledY = contentPoint.y * scaleY

                        if (scaledX in 0f..session.widthOrig.toFloat() &&
                            scaledY in 0f..session.heightOrig.toFloat()
                        ) {
                            val newPoint = CutoutHelper.PromptPoint(
                                x = scaledX,
                                y = scaledY,
                                isPositive = activeTool == ZoomablePagerImagePointTool.ADD
                            )
                            val previousPoints = promptPoints
                            val updatedPoints = promptPoints + newPoint
                            updatePointsWithHistory(updatedPoints)

                            scope.launch {
                                processingCutout = true
                                val res = session.runDecoder(updatedPoints)
                                val newCache = cutoutResult?.let { Pair(previousPoints, it) }
                                updateResultAndCache(res, newCache)
                                processingCutout = false
                            }
                        }
                    }
                } else {
                    onItemClick()
                }
            },
            onLongPress = { offset ->
                if (cutoutSession == null && !processingCutout) {
                    scope.launch {
                        processingCutout = true
                        feedbackManager.vibrate()
                        val contentPoint = zoomState.zoomable.touchPointToContentPoint(offset)

                        cutoutSession?.close()
                        val session = CutoutHelper.CutoutSession(context, media, modelManager)
                        val initOk = session.initAndRunEncoder()

                        if (initOk) {
                            cutoutSession = session
                            val contentSize = zoomState.zoomable.contentSize
                            val scaleX = if (contentSize.width > 0) session.widthOrig.toFloat() / contentSize.width.toFloat() else 1f
                            val scaleY = if (contentSize.height > 0) session.heightOrig.toFloat() / contentSize.height.toFloat() else 1f

                            val scaledX = contentPoint.x * scaleX
                            val scaledY = contentPoint.y * scaleY

                            val initialPoint = CutoutHelper.PromptPoint(
                                x = scaledX,
                                y = scaledY,
                                isPositive = true
                            )
                            val pointsList = listOf(initialPoint)
                            promptPointsHistory = listOf(pointsList)
                            historyIndex = 0
                            promptPoints = pointsList
                            activeTool = ZoomablePagerImagePointTool.ADD // Default to Include mode

                            val result = session.runDecoder(pointsList)
                            if (result != null) {
                                feedbackManager.vibrate()
                                updateResultAndCache(result, null)
                            } else {
                                session.close()
                                cutoutSession = null
                                promptPoints = emptyList()
                                promptPointsHistory = emptyList()
                                historyIndex = -1
                                updateResultAndCache(null, null)
                                Toast.makeText(context, "No object detected under long-press", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            session.close()
                            Toast.makeText(context, "Failed to initialize cutout engine", Toast.LENGTH_SHORT).show()
                        }
                        processingCutout = false
                    }
                }
            },
            alignment = Alignment.Center,
            contentDescription = media.label,
            scrollBar = null
        )

        // Dimmed background and cutout overlay
        if (cutoutSession != null) {
            val session = cutoutSession!!
            val result = cutoutResult
            val displayRect = zoomState.zoomable.contentDisplayRect
            val originSize = zoomState.zoomable.contentOriginSize

            if (displayRect.width > 0 && originSize.width > 0) {
                // Dim background (no pointerInput / no touch interception to allow zoom/pan pass-through)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // Render cutout subject, its glowing border outline, and point markers directly on a GPU-synchronized Canvas
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val displayRect = zoomState.zoomable.contentDisplayRect
                    if (displayRect.width > 0 && session.widthOrig > 0) {
                        // 1. Draw the cutout subject and its glowing border (if a cutout is active/calculated)
                        if (result != null) {
                            val screenStartX = displayRect.left + (result.originalBounds.left.toFloat() / session.widthOrig.toFloat()) * displayRect.width
                            val screenStartY = displayRect.top + (result.originalBounds.top.toFloat() / session.heightOrig.toFloat()) * displayRect.height
                            val screenWidth = ((result.originalBounds.right - result.originalBounds.left).toFloat() / session.widthOrig.toFloat()) * displayRect.width
                            val screenHeight = ((result.originalBounds.bottom - result.originalBounds.top).toFloat() / session.heightOrig.toFloat()) * displayRect.height

                            drawIntoCanvas { canvas ->
                                val nativeCanvas = canvas.nativeCanvas
                                val srcRect = android.graphics.Rect(0, 0, result.bitmap.width, result.bitmap.height)
                                
                                val strokeWidthPx = glowRadius.dp.toPx()
                                val diagOffset = (strokeWidthPx / 1.4142f).toInt()
                                val strokeWidthInt = strokeWidthPx.toInt()

                                // Draw animated white contour outline by drawing the bitmap shifted in 8 directions
                                val strokePaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    colorFilter = android.graphics.PorterDuffColorFilter(
                                        android.graphics.Color.WHITE,
                                        android.graphics.PorterDuff.Mode.SRC_IN
                                    )
                                }

                                val offsets = listOf(
                                    Pair(strokeWidthInt, 0),
                                    Pair(-strokeWidthInt, 0),
                                    Pair(0, strokeWidthInt),
                                    Pair(0, -strokeWidthInt),
                                    Pair(diagOffset, diagOffset),
                                    Pair(-diagOffset, diagOffset),
                                    Pair(diagOffset, -diagOffset),
                                    Pair(-diagOffset, -diagOffset)
                                )

                                offsets.forEach { (dx, dy) ->
                                    val dstRectShifted = android.graphics.Rect(
                                        screenStartX.toInt() + dx,
                                        screenStartY.toInt() + dy,
                                        (screenStartX + screenWidth).toInt() + dx,
                                        (screenStartY + screenHeight).toInt() + dy
                                    )
                                    nativeCanvas.drawBitmap(result.bitmap, srcRect, dstRectShifted, strokePaint)
                                }

                                // Draw original cutout bitmap on top
                                val dstRectNormal = android.graphics.Rect(
                                    screenStartX.toInt(),
                                    screenStartY.toInt(),
                                    (screenStartX + screenWidth).toInt(),
                                    (screenStartY + screenHeight).toInt()
                                )
                                val imagePaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                }
                                nativeCanvas.drawBitmap(result.bitmap, srcRect, dstRectNormal, imagePaint)
                            }
                        }

                        // 2. Draw prompt point markers
                        promptPoints.forEach { pt ->
                            val screenX = displayRect.left + (pt.x / session.widthOrig.toFloat()) * displayRect.width
                            val screenY = displayRect.top + (pt.y / session.heightOrig.toFloat()) * displayRect.height

                            val dotColor = if (pt.isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                            val glowColor = dotColor.copy(alpha = 0.3f)

                            // Glow halo (radius = 10.dp)
                            drawCircle(
                                color = glowColor,
                                radius = 10.dp.toPx(),
                                center = Offset(screenX, screenY)
                            )
                            // White border (radius = 5.dp)
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = Offset(screenX, screenY)
                            )
                            // Inner dot (radius = 3.5.dp)
                            drawCircle(
                                color = dotColor,
                                radius = 3.5.dp.toPx(),
                                center = Offset(screenX, screenY)
                            )
                        }
                    }
                }

                // Helper to run refinement action
                val runRefinedAction = { action: suspend (Bitmap) -> Unit ->
                    if (result != null) {
                        scope.launch {
                            isRefining = true
                            val refined = session.finalizeCutout(result.originalBounds)
                            isRefining = false

                            val bitmapToUse = refined?.bitmap ?: result.bitmap
                            action(bitmapToUse)

                            cutoutSession?.close()
                            cutoutSession = null
                            promptPoints = emptyList()
                            promptPointsHistory = emptyList()
                            historyIndex = -1
                            updateResultAndCache(null, null)
                            activeTool = ZoomablePagerImagePointTool.NONE
                        }
                    }
                }

                // Floating close button at top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 24.dp, top = 64.dp)
                ) {
                    FloatingCloseButton(
                        onClose = {
                            cutoutSession?.close()
                            cutoutSession = null
                            promptPoints = emptyList()
                            promptPointsHistory = emptyList()
                            historyIndex = -1
                            updateResultAndCache(null, null)
                            activeTool = ZoomablePagerImagePointTool.NONE
                        }
                    )
                }

                // Bottom Centered: Combined Controls Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                ) {
                    val canUndo = historyIndex > 0
                    val canRedo = historyIndex < promptPointsHistory.size - 1

                    CombinedCutoutControlsPill(
                        activeTool = activeTool,
                        onToolChange = { activeTool = it },
                        onReset = {
                            // Completely clear all points and stay in cutout mode
                            val previousPoints = promptPoints
                            updatePointsWithHistory(emptyList())
                            val newCache = cutoutResult?.let { Pair(previousPoints, it) }
                            updateResultAndCache(null, newCache)
                        },
                        canUndo = canUndo,
                        canRedo = canRedo,
                        onUndo = {
                            if (historyIndex > 0) {
                                val previousPoints = promptPoints
                                historyIndex--
                                val newPoints = promptPointsHistory[historyIndex]
                                promptPoints = newPoints

                                val cache = lastResultCache
                                if (cache != null && cache.first == newPoints) {
                                    // Instant cache hit swap
                                    val currentRes = cutoutResult
                                    updateResultAndCache(cache.second, currentRes?.let { Pair(previousPoints, it) })
                                } else {
                                    scope.launch {
                                        processingCutout = true
                                        val res = session.runDecoder(newPoints)
                                        val newCache = cutoutResult?.let { Pair(previousPoints, it) }
                                        updateResultAndCache(res, newCache)
                                        processingCutout = false
                                    }
                                }
                            }
                        },
                        onRedo = {
                            if (historyIndex < promptPointsHistory.size - 1) {
                                val previousPoints = promptPoints
                                historyIndex++
                                val newPoints = promptPointsHistory[historyIndex]
                                promptPoints = newPoints

                                val cache = lastResultCache
                                if (cache != null && cache.first == newPoints) {
                                    // Instant cache hit swap
                                    val currentRes = cutoutResult
                                    updateResultAndCache(cache.second, currentRes?.let { Pair(previousPoints, it) })
                                } else {
                                    scope.launch {
                                        processingCutout = true
                                        val res = session.runDecoder(newPoints)
                                        val newCache = cutoutResult?.let { Pair(previousPoints, it) }
                                        updateResultAndCache(res, newCache)
                                        processingCutout = false
                                    }
                                }
                            }
                        },
                        hasResult = result != null,
                        onCopy = { runRefinedAction { CutoutHelper.copyToClipboard(context, it) } },
                        onShare = { runRefinedAction { CutoutHelper.shareCutout(context, it) } },
                        onSave = { runRefinedAction { CutoutHelper.saveToGallery(context, it) } }
                    )
                }
            }
        }

        // Circular processing/refinement indicator
        if (processingCutout || isRefining) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {})
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isRefining) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Refining cutout...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

enum class ZoomablePagerImagePointTool { NONE, ADD, REMOVE }

@Composable
fun CombinedCutoutControlsPill(
    activeTool: ZoomablePagerImagePointTool,
    onToolChange: (ZoomablePagerImagePointTool) -> Unit,
    onReset: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    hasResult: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = if (hasResult) RoundedCornerShape(20.dp) else CircleShape,
        color = Color.Black.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
            .animateContentSize()
            .shadow(8.dp, shape = if (hasResult) RoundedCornerShape(20.dp) else CircleShape)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIconButton(
                    onClick = { onToolChange(if (activeTool == ZoomablePagerImagePointTool.ADD) ZoomablePagerImagePointTool.NONE else ZoomablePagerImagePointTool.ADD) },
                    icon = androidx.compose.material.icons.Icons.Default.Add,
                    label = "Include",
                    selected = activeTool == ZoomablePagerImagePointTool.ADD,
                    selectedColor = Color(0xFF4CAF50).copy(alpha = 0.8f)
                )
                ToolbarIconButton(
                    onClick = { onToolChange(if (activeTool == ZoomablePagerImagePointTool.REMOVE) ZoomablePagerImagePointTool.NONE else ZoomablePagerImagePointTool.REMOVE) },
                    icon = androidx.compose.material.icons.Icons.Default.Remove,
                    label = "Exclude",
                    selected = activeTool == ZoomablePagerImagePointTool.REMOVE,
                    selectedColor = Color(0xFFF44336).copy(alpha = 0.8f)
                )
                
                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = Color.White.copy(alpha = 0.15f)
                )
                
                ToolbarIconButton(
                    onClick = onUndo,
                    icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Undo,
                    label = "Undo",
                    selected = false,
                    enabled = canUndo
                )
                ToolbarIconButton(
                    onClick = onRedo,
                    icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Redo,
                    label = "Redo",
                    selected = false,
                    enabled = canRedo
                )
                
                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = Color.White.copy(alpha = 0.15f)
                )
                
                ToolbarIconButton(
                    onClick = onReset,
                    icon = androidx.compose.material.icons.Icons.Default.Refresh,
                    label = "Reset",
                    selected = false
                )
            }

            if (hasResult) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.width(200.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolbarIconButton(
                        onClick = onCopy,
                        icon = Icons.Default.ContentCopy,
                        label = "Copy",
                        selected = false
                    )
                    ToolbarIconButton(
                        onClick = onShare,
                        icon = Icons.Default.Share,
                        label = "Share",
                        selected = false
                    )
                    ToolbarIconButton(
                        onClick = onSave,
                        icon = Icons.Default.Save,
                        label = "Save",
                        selected = false
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier.shadow(8.dp, shape = CircleShape)
    ) {
        androidx.compose.material3.IconButton(
            onClick = onClose,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = "Close Cutout",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ToolbarIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    selectedColor: Color = MaterialTheme.colorScheme.primaryContainer,
    enabled: Boolean = true
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) selectedColor else Color.Transparent,
            contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.8f),
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        ),
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp)
        )
    }
}
