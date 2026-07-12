/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview.components.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dot.gallery.R

/**
 * Which prompt-point tool is currently armed while refining a subject cutout.
 * [ADD] seeds an "include" point, [REMOVE] an "exclude" point, [NONE] disables point tapping.
 */
enum class ZoomablePagerImagePointTool { NONE, ADD, REMOVE }

/** Semantic accents for include (+) / exclude (-) prompt points, shared by markers and controls. */
private val CutoutIncludeColor = Color(0xFF34C759)
private val CutoutExcludeColor = Color(0xFFFF3B30)

/**
 * Full-screen overlay drawn on top of the media viewer while a subject-cutout session is active.
 * Handles the dim scrim and the cutout subject + animated contour + prompt markers (on a Canvas).
 * The interactive controls live in the [MediaViewScreen] bottom bar ([CutoutControlsBar]); this
 * overlay is purely visual so image zoom/pan gestures keep working.
 */
@Composable
internal fun CutoutOverlay(
    state: CutoutState,
    zoomState: com.github.panpf.zoomimage.SketchZoomState,
    glowRadius: Float,
) {
    val session = state.session ?: return
    val result = state.result
    val displayRect = zoomState.zoomable.contentDisplayRect

    val strokePaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            colorFilter = android.graphics.PorterDuffColorFilter(
                android.graphics.Color.WHITE,
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }
    val imagePaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
        }
    }
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)

    if (displayRect.width > 0 && session.widthOrig > 0) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dim background (no pointerInput so zoom/pan gestures still pass through to the image).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
            )

            // Cutout subject, animated glowing contour and prompt-point markers.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rect = zoomState.zoomable.contentDisplayRect
                if (rect.width > 0 && session.widthOrig > 0) {
                    if (result != null) {
                        val screenStartX = rect.left + (result.originalBounds.left.toFloat() / session.widthOrig.toFloat()) * rect.width
                        val screenStartY = rect.top + (result.originalBounds.top.toFloat() / session.heightOrig.toFloat()) * rect.height
                        val screenWidth = ((result.originalBounds.right - result.originalBounds.left).toFloat() / session.widthOrig.toFloat()) * rect.width
                        val screenHeight = ((result.originalBounds.bottom - result.originalBounds.top).toFloat() / session.heightOrig.toFloat()) * rect.height

                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val srcRect = android.graphics.Rect(0, 0, result.bitmap.width, result.bitmap.height)

                            val strokeWidthPx = glowRadius.dp.toPx()
                            val diagOffset = (strokeWidthPx / 1.4142f).toInt()
                            val strokeWidthInt = strokeWidthPx.toInt()

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

                            val dstRectNormal = android.graphics.Rect(
                                screenStartX.toInt(),
                                screenStartY.toInt(),
                                (screenStartX + screenWidth).toInt(),
                                (screenStartY + screenHeight).toInt()
                            )
                            nativeCanvas.drawBitmap(result.bitmap, srcRect, dstRectNormal, imagePaint)
                        }
                    }

                    state.promptPoints.forEach { pt ->
                        val screenX = rect.left + (pt.x / session.widthOrig.toFloat()) * rect.width
                        val screenY = rect.top + (pt.y / session.heightOrig.toFloat()) * rect.height

                        val dotColor = if (pt.isPositive) CutoutIncludeColor else CutoutExcludeColor
                        drawCircle(
                            color = dotColor.copy(alpha = 0.3f),
                            radius = 10.dp.toPx(),
                            center = Offset(screenX, screenY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(screenX, screenY)
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 3.5.dp.toPx(),
                            center = Offset(screenX, screenY)
                        )
                    }
                }
            }

        }
    }
}

/**
 * Redesigned cutout controls, rendered in the [MediaViewScreen] bottom bar (replacing the quick
 * actions) while a session is active. Two tiers: a tools row (close, include/exclude, undo/redo/
 * reset) and — once a mask exists — an export row (copy/share/save) that animates in below it.
 */
@Composable
internal fun CutoutControlsBar(
    controller: CutoutController,
    modifier: Modifier = Modifier
) {
    val state = controller.state
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier.animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tier 1 — editing tools.
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CutoutToolButton(
                    onClick = controller.onClose,
                    icon = Icons.Outlined.Close,
                    label = stringResource(R.string.cutout_close)
                )

                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                CutoutToolButton(
                    onClick = {
                        controller.onToolChange(
                            if (state.activeTool == ZoomablePagerImagePointTool.ADD) ZoomablePagerImagePointTool.NONE
                            else ZoomablePagerImagePointTool.ADD
                        )
                    },
                    icon = Icons.Outlined.Add,
                    label = stringResource(R.string.cutout_include),
                    selected = state.activeTool == ZoomablePagerImagePointTool.ADD,
                    selectedColor = CutoutIncludeColor
                )
                CutoutToolButton(
                    onClick = {
                        controller.onToolChange(
                            if (state.activeTool == ZoomablePagerImagePointTool.REMOVE) ZoomablePagerImagePointTool.NONE
                            else ZoomablePagerImagePointTool.REMOVE
                        )
                    },
                    icon = Icons.Outlined.Remove,
                    label = stringResource(R.string.cutout_exclude),
                    selected = state.activeTool == ZoomablePagerImagePointTool.REMOVE,
                    selectedColor = CutoutExcludeColor
                )

                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                CutoutToolButton(
                    onClick = controller.onUndo,
                    icon = Icons.AutoMirrored.Outlined.Undo,
                    label = stringResource(R.string.cutout_undo),
                    enabled = state.canUndo
                )
                CutoutToolButton(
                    onClick = controller.onRedo,
                    icon = Icons.AutoMirrored.Outlined.Redo,
                    label = stringResource(R.string.cutout_redo),
                    enabled = state.canRedo
                )
                CutoutToolButton(
                    onClick = controller.onReset,
                    icon = Icons.Outlined.Refresh,
                    label = stringResource(R.string.cutout_reset)
                )
            }

            // Tier 2 — export actions, available once a mask exists.
            AnimatedVisibility(visible = state.hasResult) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .width(220.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CutoutToolButton(
                            onClick = controller.onCopy,
                            icon = Icons.Outlined.ContentCopy,
                            label = stringResource(R.string.cutout_copy)
                        )
                        CutoutToolButton(
                            onClick = controller.onShare,
                            icon = Icons.Outlined.Share,
                            label = stringResource(R.string.cutout_share)
                        )
                        CutoutToolButton(
                            onClick = controller.onSave,
                            icon = Icons.Outlined.Save,
                            label = stringResource(R.string.cutout_save)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Non-intrusive hint drawn when background detection ([SubjectSuggestionState]) has found a likely
 * subject: a pulsing outline around the detected bounds plus a single "cut out" chip. Tapping the
 * chip ([onAccept]) promotes the already-computed mask into a full refine session — no re-encoding.
 * Draws no scrim and intercepts no image gestures except the chip itself.
 */
@Composable
internal fun SubjectSuggestionOverlay(
    state: SubjectSuggestionState,
    zoomState: com.github.panpf.zoomimage.SketchZoomState,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestion = state.suggestion ?: return
    val bounds = suggestion.result.originalBounds
    val wOrig = suggestion.session.widthOrig
    val hOrig = suggestion.session.heightOrig
    if (wOrig <= 0 || hOrig <= 0) return

    val transition = rememberInfiniteTransition(label = "subjectSuggestion")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subjectSuggestionPulse"
    )
    // One-shot "bloom": when a new subject is found, animate the border/glow into existence.
    val reveal = remember(suggestion) { Animatable(0f) }
    LaunchedEffect(suggestion) {
        reveal.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }
    val accent = MaterialTheme.colorScheme.primary
    val accentArgb = accent.toArgb()
    val bitmap = suggestion.result.bitmap

    // Tint the (transparent-background) mask bitmap with the accent colour so its opaque silhouette
    // can be stamped as an outline. Rebuilt only if the theme accent changes.
    val outlinePaint = remember(accentArgb) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            colorFilter = android.graphics.PorterDuffColorFilter(
                accentArgb,
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }
    // Punches the subject's interior back out of the dilated silhouette, leaving only a fine border
    // that traces the actual subject edges.
    val erasePaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // No gesture handling here: tapping the subject is routed through the image's own onTap
        // (see ZoomablePagerImage) so zoom/pan gestures keep working. Only the chip below is clickable.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rect = zoomState.zoomable.contentDisplayRect
            if (rect.width <= 0f || rect.height <= 0f) return@Canvas
            val left = rect.left + (bounds.left.toFloat() / wOrig) * rect.width
            val top = rect.top + (bounds.top.toFloat() / hOrig) * rect.height
            val right = rect.left + (bounds.right.toFloat() / wOrig) * rect.width
            val bottom = rect.top + (bounds.bottom.toFloat() / hOrig) * rect.height

            val revealV = reveal.value
            val strokePx = 2.5.dp.toPx()
            // Glow grows outward as the bloom plays in, then gently breathes with the pulse.
            val glowMax = 14.dp.toPx() * revealV

            // Stamp the mask silhouette shifted in 8 directions at [radius]; this dilates the actual
            // subject shape (never a rectangle) so both glow and border follow its contour.
            fun android.graphics.Canvas.stampRing(
                bmp: android.graphics.Bitmap,
                srcRect: android.graphics.Rect,
                dstOf: (Float, Float) -> android.graphics.RectF,
                radius: Float,
                paint: android.graphics.Paint
            ) {
                if (radius < 0.5f) return
                val d = radius / 1.4142f
                val dirs = arrayOf(
                    radius to 0f, -radius to 0f, 0f to radius, 0f to -radius,
                    d to d, -d to d, d to -d, -d to -d
                )
                dirs.forEach { (dx, dy) -> drawBitmap(bmp, srcRect, dstOf(dx, dy), paint) }
            }

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val src = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                fun dst(dx: Float, dy: Float) = android.graphics.RectF(
                    left + dx, top + dy, right + dx, bottom + dy
                )

                // Isolate on its own layer so DST_OUT carves only within the drawn silhouette.
                val pad = glowMax + strokePx + 2f
                val layer = native.saveLayer(left - pad, top - pad, right + pad, bottom + pad, null)

                // Soft silhouette-shaped glow: concentric dilations fading outward.
                val rings = 3
                for (i in rings downTo 1) {
                    val rad = glowMax * i / rings
                    val ringAlpha = (pulse * 0.22f * (1f - (i - 1f) / rings)).coerceIn(0f, 1f)
                    outlinePaint.alpha = (ringAlpha * 255f).toInt()
                    native.stampRing(bitmap, src, ::dst, rad, outlinePaint)
                }

                // Crisp accent border tracing the subject edge.
                outlinePaint.alpha = ((0.55f + 0.45f * pulse) * revealV * 255f).toInt().coerceIn(0, 255)
                native.stampRing(bitmap, src, ::dst, strokePx, outlinePaint)

                // Carve out the interior so only the border + outer glow remain over the subject.
                native.drawBitmap(bitmap, src, dst(0f, 0f), erasePaint)
                native.restoreToCount(layer)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.95f), CircleShape)
                .clickable(onClick = onAccept)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.cutout_action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * True when [position] (in screen pixels) falls on an opaque pixel of the detected subject's mask.
 * Maps the tap back into the mask bitmap via the image's on-screen [bounds] rect and samples alpha,
 * so only hits on the actual silhouette (not the surrounding bounding box) count. Falls back to a
 * bounding-box test if the bitmap can't be sampled (e.g. a hardware-backed config).
 */
internal fun hitsSubject(
    position: Offset,
    zoomState: com.github.panpf.zoomimage.SketchZoomState,
    bounds: android.graphics.Rect,
    wOrig: Int,
    hOrig: Int,
    bitmap: android.graphics.Bitmap
): Boolean {
    val rect = zoomState.zoomable.contentDisplayRect
    if (rect.width <= 0f || rect.height <= 0f) return false
    val left = rect.left + (bounds.left.toFloat() / wOrig) * rect.width
    val top = rect.top + (bounds.top.toFloat() / hOrig) * rect.height
    val right = rect.left + (bounds.right.toFloat() / wOrig) * rect.width
    val bottom = rect.top + (bounds.bottom.toFloat() / hOrig) * rect.height
    if (position.x < left || position.x > right || position.y < top || position.y > bottom) return false

    val u = ((position.x - left) / (right - left) * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val v = ((position.y - top) / (bottom - top) * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    return runCatching { android.graphics.Color.alpha(bitmap.getPixel(u, v)) > 32 }.getOrDefault(true)
}

/** A single toolbar button with a tooltip, used inside [CutoutControlsBar]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CutoutToolButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    val tooltipState = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
            4.dp
        ),
        tooltip = { PlainTooltip { Text(text = label) } },
        state = tooltipState
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (selected) selectedColor else Color.Transparent,
                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            ),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
