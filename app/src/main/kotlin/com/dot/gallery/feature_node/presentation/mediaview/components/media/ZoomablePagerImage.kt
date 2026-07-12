/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dot.gallery.cloud.core.CloudRuntimeSettings
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.image.CloudImageSource
import com.dot.gallery.core.Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION
import com.dot.gallery.core.Settings
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
import com.github.panpf.zoomimage.compose.subsampling.ComposeTileImage
import com.github.panpf.zoomimage.compose.subsampling.SubsamplingState
import com.github.panpf.zoomimage.compose.zoom.ZoomableState
import com.github.panpf.zoomimage.compose.zoom.mouseZoom
import com.github.panpf.zoomimage.compose.zoom.zoomable
import com.github.panpf.zoomimage.rememberSketchZoomState
import com.github.panpf.zoomimage.subsampling.SubsamplingImage
import com.github.panpf.zoomimage.util.IntSizeCompat
import com.github.panpf.zoomimage.util.isNotEmpty
import com.github.panpf.zoomimage.zoom.ContentScaleCompat
import com.github.panpf.zoomimage.zoom.ScalesCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ScreenRotationAlt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import com.dot.gallery.core.ml.CutoutHelper
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing

// Extended max zoom: allow zooming in until each source pixel is shown at this many screen pixels,
// i.e. up to 5000% of the image's native (1:1) resolution. Double-tap zoom is unaffected — it still
// toggles between the fit scale and the default dynamic mediumScale (see ExtendedZoomScalesCalculator).
private const val EXTENDED_MAX_NATIVE_SCALE = 50f

/**
 * Keeps zoomimage's default dynamic minScale/mediumScale (so double-tap behaves normally) but raises
 * the pinch ceiling (maxScale) to [maxNativeMultiple] times the image's native 1:1 resolution.
 *
 * At scale == contentOriginSize / contentSize the content is displayed at its native pixel density,
 * so multiplying that by [maxNativeMultiple] yields "N× native". When the native size is unknown
 * (no subsampling), the painter is already native, so 1.0 is used as the native scale.
 */
private class ExtendedZoomScalesCalculator(
    private val maxNativeMultiple: Float,
) : ScalesCalculator {

    private val base = ScalesCalculator.Dynamic

    @Suppress("DEPRECATION")
    override fun calculate(
        containerSize: IntSizeCompat,
        contentSize: IntSizeCompat,
        contentOriginSize: IntSizeCompat,
        contentScale: ContentScaleCompat,
        minScale: Float,
        initialScale: Float,
    ): ScalesCalculator.Result {
        val baseResult = base.calculate(
            containerSize = containerSize,
            contentSize = contentSize,
            contentOriginSize = contentOriginSize,
            contentScale = contentScale,
            minScale = minScale,
            initialScale = initialScale,
        )
        val nativeScale = if (contentOriginSize.isNotEmpty() && contentSize.isNotEmpty()) {
            maxOf(
                contentOriginSize.width / contentSize.width.toFloat(),
                contentOriginSize.height / contentSize.height.toFloat(),
            )
        } else {
            1f
        }
        val extendedMax = maxOf(
            baseResult.maxScale,
            baseResult.mediumScale,
            nativeScale * maxNativeMultiple,
        )
        return ScalesCalculator.Result(
            minScale = baseResult.minScale,
            mediumScale = baseResult.mediumScale,
            maxScale = extendedMax,
        )
    }
}

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
                    // Bust the cache when the underlying file changes (#1004).
                    setExtra(key = "mediaVersion", value = "${media.timestamp}:${media.size}")
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
    onSubsamplingLoadingChange: (Boolean) -> Unit = {},
    onCutoutStateChanged: (Boolean) -> Unit = {},
    isSelected: Boolean = true,
    uiVisible: Boolean = true
) {
    val feedbackManager = rememberFeedbackManager()
    var isRotating by rememberSaveable(media) { mutableStateOf(false) }
    var currentRotation by rememberSaveable(media) { mutableIntStateOf(0) }
    val rotationAnimation by animateFloatAsState(
        targetValue = if (isRotating) 90f else 0f,
        label = "rotationAnimation"
    )
    val zoomState = rememberSketchZoomState()
    // Raise the maximum (pinch) zoom to 5000% of native while leaving double-tap on the normal
    // dynamic mediumScale. See ExtendedZoomScalesCalculator.
    LaunchedEffect(zoomState) {
        zoomState.zoomable.setScalesCalculator(ExtendedZoomScalesCalculator(EXTENDED_MAX_NATIVE_SCALE))
    }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val mediaUri = remember(media) {
        media.getUri().toString()
    }
    // A content-version token folded into the Sketch cache key so the memory and
    // disk result caches are tied to the current file bytes. After editing or
    // overwriting an image its URI is unchanged, so without this the viewer kept
    // serving the stale original from cache while the Glide grid (keyed on
    // media.toString()) had already refreshed (#1004).
    val mediaVersion = remember(media) { "${media.timestamp}:${media.size}" }
    val isEncrypted = remember(media) {
        media.isEncrypted
    }
    val isJxl = remember(media) { media.isJxl }
    val isAnimated = remember(media) {
        media.isApng || media.isJxl || (media.isAvif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }
    // Pixel-perfect (nearest-neighbor) rendering for pixel art: when enabled we draw the image
    // with FilterQuality.None so zooming shows crisp square pixels instead of a bilinear-smoothed
    // blur. ZoomImage scales via a GPU graphicsLayer (always linearly filtered), so the setting is
    // honored through a dedicated canvas-transform draw path (see PixelPerfectZoomImage) rather than
    // ZoomImage. Animated images keep the default smooth path.
    val disableSmoothing by Settings.Misc.rememberDisableSmoothing()
    val usePixelPerfect = disableSmoothing && !isAnimated
    val filterQuality = if (disableSmoothing) FilterQuality.None else DrawScope.DefaultFilterQuality
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
            setExtra(key = "mediaVersion", value = mediaVersion)
            if (isEncrypted) {
                setExtra(key = "mediaKeyPreviewEnc", value = media.idLessKey)
            }
        },
        contentScale = ContentScale.Fit,
        filterQuality = filterQuality
    )

    // Full-res painter with state tracking
    val fullImageState = rememberAsyncImageState()
    val fullPainter = rememberAsyncImagePainter(
        request = ComposableImageRequest(mediaUri) {
            if (isEncrypted || isAnimated) {
                crossfade(durationMillis = 200)
            }
            setExtra("realMimeType", media.mimeType)
            setExtra(key = "mediaVersion", value = mediaVersion)
            if (isEncrypted) {
                setExtra(key = "mediaKeyPreviewEnc", value = media.idLessKey)
            }
        },
        state = fullImageState,
        contentScale = ContentScale.Fit,
        filterQuality = filterQuality
    )

    val isFullImageLoaded by rememberedDerivedState(media) {
        fullImageState.painterState is PainterState.Success
    }
    val activePainter = remember(isFullImageLoaded) {
        if (isFullImageLoaded) fullPainter else previewPainter
    }

    val isCloudMedia = remember(media) { media.isCloud }

    // Subsampling is set up the same way for both the smooth and pixel-perfect paths so that
    // high-resolution images retain native detail when zoomed. The difference is only in how the
    // tiles are drawn: PixelPerfectZoomImage draws them via a canvas transform with
    // FilterQuality.None (nearest-neighbor) instead of the smoothed default.
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
            // Respect the cloud "Load original image" viewer setting: when off, skip the
            // full-resolution original download and keep the lightweight preview (data saver).
            if (!CloudRuntimeSettings.loadOriginalImage) return@LaunchedEffect
            val uri = media.getUri()
            val providerName = uri.authority ?: return@LaunchedEffect
            val providerType = try { ProviderType.valueOf(providerName) } catch (_: Exception) { return@LaunchedEffect }
            // remoteId may contain slashes (SMB/NFS/WebDAV paths like "Photos/IMG.jpg"); pathSegments
            // .first() would truncate it to the folder and request the directory as the original.
            val remoteId = uri.path?.trimStart('/')?.takeIf { it.isNotEmpty() } ?: return@LaunchedEffect
            val configId = uri.getQueryParameter("cfg")?.toLongOrNull() ?: -1L
            // Signal that the full-size original is being fetched for subsampling so the UI can show
            // a subtle loading indicator. try/finally guarantees the flag is cleared on success,
            // failure, or cancellation (e.g. swiping to another page mid-download).
            onSubsamplingLoadingChange(true)
            val cloudSource = try {
                CloudImageSource.create(context, providerType, remoteId, configId)
            } catch (_: Exception) {
                return@LaunchedEffect
            } finally {
                onSubsamplingLoadingChange(false)
            }
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

    val imageModifier = Modifier
        .fillMaxSize()
        .swipe(onSwipeDown = onSwipeDown)
        .graphicsLayer {
            rotationZ = if (isRotating) rotationAnimation else 0f
        }
        .then(modifier)

    // Rotate the image 90° (visual step; MediaViewScreen persists it via the rotate chip).
    val rotateImage: () -> Unit = {
        if (!rotationDisabled) {
            scope.launch {
                isRotating = true
                feedbackManager.vibrate()
                currentRotation += 90
                onImageRotated(currentRotation)
                delay(350)
                zoomState.zoomable.rotate(currentRotation)
                isRotating = false
            }
        }
    }

    // --- Subject cutout (on-device MobileSAM) ---
    val modelManager = remember { (context.applicationContext as com.dot.gallery.GalleryApp).modelManager }
    val cutoutState = rememberCutoutState()
    // When true, long-press starts a cutout session and Rotate is offered as an on-screen pill.
    // When false (default), long-press rotates and Cut out is offered as an on-screen pill.
    val longPressStartsCutout by Settings.Misc.rememberLongPressCutout()

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
            cutoutState.dismiss()
        }
    }

    BackHandler(enabled = cutoutState.isActive) {
        cutoutState.dismiss()
    }

    LaunchedEffect(cutoutState.isActive, isSelected) {
        if (isSelected) {
            onCutoutStateChanged(cutoutState.isActive)
        }
    }

    // Start a cutout session seeded at the given screen-space point.
    val startCutoutAt: (Offset) -> Unit = { offset ->
        if (!cutoutState.isActive && !cutoutState.isProcessing) {
            scope.launch {
                cutoutState.isProcessing = true
                try {
                    feedbackManager.vibrate()
                    val contentPoint = zoomState.zoomable.touchPointToContentPoint(offset)

                    val session = CutoutHelper.CutoutSession(context, media, modelManager)
                    val initOk = session.initAndRunEncoder()

                    if (initOk) {
                        val contentSize = zoomState.zoomable.contentSize
                        val scaleX = if (contentSize.width > 0) session.widthOrig.toFloat() / contentSize.width.toFloat() else 1f
                        val scaleY = if (contentSize.height > 0) session.heightOrig.toFloat() / contentSize.height.toFloat() else 1f

                        val scaledX = contentPoint.x * scaleX
                        val scaledY = contentPoint.y * scaleY

                        val initialPoint = CutoutHelper.PromptPoint(x = scaledX, y = scaledY, isPositive = true)
                        val pointsList = listOf(initialPoint)
                        val result = session.runDecoder(pointsList)

                        if (result != null) {
                            // Only hoist session into state once we have a valid mask
                            cutoutState.initSession(session, pointsList)
                            feedbackManager.vibrate()
                            cutoutState.updateResult(result, null)
                        } else {
                            // No mask found — clean up without touching cutoutState
                            session.close()
                            Toast.makeText(context, context.getString(R.string.cutout_no_object), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        session.close()
                        Toast.makeText(context, context.getString(R.string.cutout_init_failed), Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    // Always reset — even on OOM, ONNX crash, or coroutine cancellation
                    cutoutState.isProcessing = false
                }
            }
        }
    }

    // Tap handler: while a cutout session is active, add/remove a prompt point; otherwise pass through.
    val onImageTap: (Offset) -> Unit = { offset ->
        val session = cutoutState.session
        if (session != null) {
            val displayRect = zoomState.zoomable.contentDisplayRect
            val isInsideImage = offset.x >= displayRect.left &&
                    offset.x <= displayRect.right &&
                    offset.y >= displayRect.top &&
                    offset.y <= displayRect.bottom

            if (isInsideImage && (cutoutState.activeTool == ZoomablePagerImagePointTool.ADD || cutoutState.activeTool == ZoomablePagerImagePointTool.REMOVE)) {
                val contentPoint = zoomState.zoomable.touchPointToContentPoint(offset)
                val contentSize = zoomState.zoomable.contentSize
                val scaleX = if (contentSize.width > 0) session.widthOrig.toFloat() / contentSize.width.toFloat() else 1f
                val scaleY = if (contentSize.height > 0) session.heightOrig.toFloat() / contentSize.height.toFloat() else 1f

                val scaledX = contentPoint.x * scaleX
                val scaledY = contentPoint.y * scaleY

                if (scaledX in 0f..session.widthOrig.toFloat() && scaledY in 0f..session.heightOrig.toFloat()) {
                    val newPoint = CutoutHelper.PromptPoint(
                        x = scaledX,
                        y = scaledY,
                        isPositive = cutoutState.activeTool == ZoomablePagerImagePointTool.ADD
                    )
                    val previousPoints = cutoutState.promptPoints
                    val updatedPoints = cutoutState.promptPoints + newPoint
                    cutoutState.pushPoints(updatedPoints)

                    scope.launch {
                        cutoutState.isProcessing = true
                        try {
                            val res = session.runDecoder(updatedPoints)
                            val newCache = cutoutState.result?.let { Pair(previousPoints, it) }
                            cutoutState.updateResult(res, newCache)
                        } finally {
                            cutoutState.isProcessing = false
                        }
                    }
                }
            }
        } else {
            onItemClick()
        }
    }

    // Configurable long-press action (default: rotate).
    val onImageLongPress: (Offset) -> Unit = { offset ->
        if (longPressStartsCutout) startCutoutAt(offset) else rotateImage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (usePixelPerfect) {
            // Pixel-art path: nearest-neighbor rendering with subsampling.
            PixelPerfectZoomImage(
                zoomable = zoomState.zoomable,
                subsampling = zoomState.subsampling,
                painter = activePainter,
                modifier = imageModifier,
                contentDescription = media.label,
                onTap = onImageTap,
                onLongPress = onImageLongPress,
            )
        } else {
            ZoomImage(
                zoomState = zoomState,
                painter = activePainter,
                modifier = imageModifier,
                onTap = onImageTap,
                onLongPress = onImageLongPress,
                alignment = Alignment.Center,
                contentDescription = media.label,
                scrollBar = null
            )
        }

        // Cutout mask, contour, controls (renders on top of either image path).
        CutoutOverlay(
            state = cutoutState,
            zoomState = zoomState,
            glowRadius = glowRadius,
            onCopy = { CutoutHelper.copyToClipboard(context, it) },
            onShare = { CutoutHelper.shareCutout(context, it) },
            onSave = { CutoutHelper.saveToGallery(context, it) }
        )

        // Processing / refinement indicator
        if (cutoutState.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {})
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.cutout_refining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        // The action NOT bound to long-press is offered as an on-screen pill, shown only while the
        // UI chrome is visible on the current page and no cutout session is in progress.
        val showActionPill = isSelected && uiVisible && !cutoutState.isActive && !cutoutState.isProcessing
        AnimatedVisibility(
            visible = showActionPill && (longPressStartsCutout || !rotationDisabled),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        ) {
            if (longPressStartsCutout) {
                // Long-press does cutout → offer Rotate as a pill.
                MediaViewActionPill(
                    icon = Icons.Outlined.ScreenRotationAlt,
                    label = stringResource(R.string.rotate),
                    onClick = rotateImage
                )
            } else {
                // Long-press rotates → offer Cut out as a pill.
                MediaViewActionPill(
                    icon = Icons.Outlined.AutoAwesome,
                    label = stringResource(R.string.cutout_action),
                    onClick = {
                        val rect = zoomState.zoomable.contentDisplayRect
                        startCutoutAt(
                            Offset(
                                rect.left + rect.width / 2f,
                                rect.top + rect.height / 2f
                            )
                        )
                    }
                )
            }
        }
    }
}

/**
 * Small pill button matching the media viewer's motion-photo / rotate chip style, used to expose
 * whichever long-press action (rotate or cut out) is not currently bound to the long-press gesture.
 */
@Composable
private fun MediaViewActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.85f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A drop-in replacement for [com.github.panpf.zoomimage.ZoomImage] that renders with
 * nearest-neighbor filtering (no smoothing) so pixel art stays crisp when zoomed, **while still
 * using subsampling** to keep native detail on high-resolution images.
 *
 * Why a custom composable instead of `ZoomImage`: `ZoomImage` applies its zoom via a GPU
 * `graphicsLayer` and draws its subsampling tiles with a hardcoded, private paint. Neither can be
 * told to skip smoothing. Here we instead reproduce the same [ZoomableState.transform] directly on
 * the draw canvas — canvas transforms honor the per-draw [FilterQuality], exactly like the
 * library's own Android View implementation which draws tiles with a `Matrix` + `Paint`. We reuse
 * [ZoomableState] only for gesture handling and read [SubsamplingState]'s public tile snapshots to
 * draw them ourselves.
 *
 * Coordinate spaces (mirrors `ZoomImage`):
 * - the base [painter] is in *content* coordinates (its own pixel size); the transform maps
 *   content -> screen.
 * - subsampling tiles ([TileSnapshot.srcRect]) are in *origin* (full-resolution) coordinates, so
 *   they are additionally scaled by contentSize/contentOriginSize before the same transform.
 */
@Composable
private fun PixelPerfectZoomImage(
    zoomable: ZoomableState,
    subsampling: SubsamplingState,
    painter: Painter,
    modifier: Modifier,
    contentDescription: String?,
    onTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
) {
    zoomable.setContentScale(ContentScale.Fit)
    zoomable.setAlignment(Alignment.Center)
    zoomable.setLayoutDirection(LocalLayoutDirection.current)
    val intrinsicSize = painter.intrinsicSize
    val contentSize = remember(intrinsicSize) {
        if (intrinsicSize.isSpecified) {
            IntSize(intrinsicSize.width.roundToInt(), intrinsicSize.height.roundToInt())
        } else {
            IntSize.Zero
        }
    }
    zoomable.setContentSize(contentSize)

    BoxWithConstraints(modifier = modifier.mouseZoom(zoomable)) {
        val density = LocalDensity.current
        val containerSize = remember(density, maxWidth, maxHeight) {
            IntSize(
                width = with(density) { maxWidth.toPx() }.roundToInt(),
                height = with(density) { maxHeight.toPx() }.roundToInt()
            )
        }
        zoomable.setContainerSize(containerSize)

        Box(
            Modifier
                .matchParentSize()
                .clipToBounds()
                .zoomable(
                    zoomable = zoomable,
                    userSetupContentSize = true,
                    onLongPress = { onLongPress(it) },
                    onTap = { onTap(it) }
                )
                .drawWithContent {
                    val transform = zoomable.transform
                    val drawSize = if (contentSize.width > 0 && contentSize.height > 0) {
                        Size(contentSize.width.toFloat(), contentSize.height.toFloat())
                    } else {
                        size
                    }
                    withTransform({
                        translate(transform.offsetX, transform.offsetY)
                        scale(
                            scaleX = transform.scaleX,
                            scaleY = transform.scaleY,
                            pivot = Offset(
                                x = transform.scaleOriginX * size.width,
                                y = transform.scaleOriginY * size.height
                            )
                        )
                        rotate(
                            degrees = transform.rotation,
                            pivot = Offset(
                                x = transform.rotationOriginX * size.width,
                                y = transform.rotationOriginY * size.height
                            )
                        )
                    }) {
                        // Base image (content coords). Visible until/where tiles cover it.
                        with(painter) {
                            draw(size = drawSize)
                        }
                        // Subsampling tiles (origin coords -> content coords), drawn crisp.
                        drawSubsamplingTiles(subsampling, zoomable)
                    }
                }
                .semantics {
                    if (contentDescription != null) {
                        this.contentDescription = contentDescription
                        this.role = Role.Image
                    }
                }
        )
    }
}

/**
 * Draws [SubsamplingState]'s foreground/background tiles with [FilterQuality.None]. Must be called
 * inside a [withTransform] block that already maps content coordinates to screen (see
 * [PixelPerfectZoomImage]); this only adds the origin->content scale that the library's
 * `firstScaleByContentSize` layer normally applies.
 */
private fun DrawScope.drawSubsamplingTiles(
    subsampling: SubsamplingState,
    zoomable: ZoomableState,
) {
    val foregroundTiles = subsampling.foregroundTiles
    if (foregroundTiles.isEmpty()) return
    val loadRect = subsampling.imageLoadRect
    if (loadRect.width <= 0 || loadRect.height <= 0) return
    val contentSize = zoomable.contentSize
    val originSize = zoomable.contentOriginSize
    if (contentSize.width <= 0 || contentSize.height <= 0) return
    if (originSize.width <= 0 || originSize.height <= 0) return

    val originToContentX = contentSize.width.toFloat() / originSize.width
    val originToContentY = contentSize.height.toFloat() / originSize.height

    fun drawTile(tile: com.github.panpf.zoomimage.subsampling.TileSnapshot) {
        val srcRect = tile.srcRect
        // Skip tiles outside the currently loaded area (matches the library's overlap check).
        if (srcRect.right <= loadRect.left || srcRect.left >= loadRect.right ||
            srcRect.bottom <= loadRect.top || srcRect.top >= loadRect.bottom
        ) return
        val tileImage = tile.tileImage
        if (tileImage == null || tileImage.isRecycled) return
        val bitmap = (tileImage as? ComposeTileImage)?.bitmap ?: return
        val dstLeft = (srcRect.left * originToContentX).roundToInt()
        val dstTop = (srcRect.top * originToContentY).roundToInt()
        val dstRight = (srcRect.right * originToContentX).roundToInt()
        val dstBottom = (srcRect.bottom * originToContentY).roundToInt()
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(dstLeft, dstTop),
            dstSize = IntSize(dstRight - dstLeft, dstBottom - dstTop),
            alpha = tile.alpha / 255f,
            filterQuality = FilterQuality.None,
        )
    }

    // Background tiles first (lower sample sizes), then the sharp foreground tiles on top.
    subsampling.backgroundTiles.forEach { drawTile(it) }
    foregroundTiles.forEach { drawTile(it) }
}
