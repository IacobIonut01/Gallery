package com.dot.gallery.feature_node.presentation.edit.components.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dot.gallery.feature_node.presentation.mediaview.components.media.CutoutState
import com.dot.gallery.feature_node.presentation.mediaview.components.media.ZoomablePagerImagePointTool
import kotlin.math.min

/** Semantic accents for include (+) / exclude (-) prompt points. */
private val IncludeColor = Color(0xFF34C759)
private val ExcludeColor = Color(0xFFFF3B30)

/**
 * Interactive subject-cutout surface for the editor, analogous to [MarkupPainter]. Renders the
 * working [bitmap] fit-to-box, maps taps into bitmap pixel coordinates ([onAddPoint]), and draws
 * the current mask + prompt markers over a dim scrim. Coordinate mapping is a plain centered fit
 * (no zoom/pan) so hit-testing is deterministic.
 */
@Composable
fun CutoutPainter(
    bitmap: Bitmap,
    cutoutState: CutoutState,
    onAddPoint: (x: Float, y: Float, isPositive: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val result = cutoutState.result
    val session = cutoutState.session

    // White "glow" contour paint (tints the subject silhouette white so offset copies form an
    // outline) + a plain paint for the normal subject draw — mirrors the media viewer's overlay.
    val strokePaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            colorFilter = android.graphics.PorterDuffColorFilter(
                android.graphics.Color.WHITE,
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }
    val imagePaint = remember { android.graphics.Paint().apply { isAntiAlias = true } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        val bmpW = bitmap.width.toFloat().coerceAtLeast(1f)
        val bmpH = bitmap.height.toFloat().coerceAtLeast(1f)
        val scale = min(boxW / bmpW, boxH / bmpH)
        val dispW = bmpW * scale
        val dispH = bmpH * scale
        val offsetX = (boxW - dispW) / 2f
        val offsetY = (boxH - dispH) / 2f

        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(session, cutoutState.activeTool, bitmap) {
                    detectTapGestures { tap ->
                        val bx = ((tap.x - offsetX) / scale)
                        val by = ((tap.y - offsetY) / scale)
                        if (bx in 0f..bmpW && by in 0f..bmpH) {
                            onAddPoint(bx, by, cutoutState.activeTool != ZoomablePagerImagePointTool.REMOVE)
                        }
                    }
                }
        )

        // Dim scrim + mask subject + markers once a session exists.
        if (session != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val wOrig = session.widthOrig.toFloat().coerceAtLeast(1f)
                val hOrig = session.heightOrig.toFloat().coerceAtLeast(1f)
                if (result != null && !result.bitmap.isRecycled) {
                    val b = result.originalBounds
                    val left = offsetX + (b.left / wOrig) * dispW
                    val top = offsetY + (b.top / hOrig) * dispH
                    val right = offsetX + (b.right / wOrig) * dispW
                    val bottom = offsetY + (b.bottom / hOrig) * dispH
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        val src = android.graphics.Rect(0, 0, result.bitmap.width, result.bitmap.height)

                        // Glowing white contour: draw the silhouette tinted white, offset in 8
                        // directions, so the union forms an outline around the subject.
                        val stroke = 3.dp.toPx()
                        val diag = (stroke / 1.4142f).toInt()
                        val s = stroke.toInt()
                        val offsets = listOf(
                            s to 0, -s to 0, 0 to s, 0 to -s,
                            diag to diag, -diag to diag, diag to -diag, -diag to -diag
                        )
                        offsets.forEach { (dx, dy) ->
                            val dst = android.graphics.Rect(
                                left.toInt() + dx, top.toInt() + dy,
                                right.toInt() + dx, bottom.toInt() + dy
                            )
                            nativeCanvas.drawBitmap(result.bitmap, src, dst, strokePaint)
                        }

                        val dst = android.graphics.Rect(
                            left.toInt(), top.toInt(), right.toInt(), bottom.toInt()
                        )
                        nativeCanvas.drawBitmap(result.bitmap, src, dst, imagePaint)
                    }
                }
                cutoutState.promptPoints.forEach { pt ->
                    val sx = offsetX + (pt.x / wOrig) * dispW
                    val sy = offsetY + (pt.y / hOrig) * dispH
                    val dot = if (pt.isPositive) IncludeColor else ExcludeColor
                    drawCircle(dot.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = Offset(sx, sy))
                    drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(sx, sy))
                    drawCircle(dot, radius = 3.5.dp.toPx(), center = Offset(sx, sy))
                }
            }
        }

        if (cutoutState.isProcessing) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
