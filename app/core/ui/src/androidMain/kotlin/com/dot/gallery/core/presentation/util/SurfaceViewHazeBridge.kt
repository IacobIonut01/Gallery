package com.dot.gallery.core.presentation.util

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RenderEffect
import androidx.core.graphics.createBitmap
import android.graphics.Shader
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean



/**
 * Finds the first video output descendant in a view hierarchy — either a [SurfaceView]
 * (captured via [PixelCopy]) or a [TextureView] (captured via [TextureView.getBitmap],
 * which needs no surface access).
 */
private fun View.findVideoOutputView(): View? {
    if (this is SurfaceView || this is TextureView) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val found = getChildAt(i).findVideoOutputView()
            if (found != null) return found
        }
    }
    return null
}

/**
 * Captures a video output view's content into a tiny bitmap (~48 px wide) at high
 * frequency (~50 ms) — [SurfaceView] via [PixelCopy], [TextureView] via
 * [TextureView.getBitmap]. The bitmap is so small (~7 KB) that capture is near-free.
 *
 * @param view       View (or parent) whose first SurfaceView/TextureView descendant is captured.
 * @param enabled    Toggle capture on/off.
 * @param captureWidth Target width in pixels. Height is derived from aspect ratio.
 * @param intervalMs Milliseconds between capture attempts.
 */
@Composable
fun rememberSurfaceCapture(
    view: View?,
    enabled: Boolean = true,
    captureWidth: Int = 48,
    intervalMs: Long = 50L
): State<ImageBitmap?> {
    val state = remember { mutableStateOf<ImageBitmap?>(null) }
    // Read the latest `enabled` inside the long-lived loop so toggling it does not
    // depend on the LaunchedEffect restarting (which is unreliable when this lives
    // inside AnimatedVisibility content under a non-restartable composable).
    val enabledState = rememberUpdatedState(enabled)

    val pixelCopyThread = remember {
        HandlerThread("PixelCopyThread").apply { start() }
    }
    val pixelCopyHandler = remember(pixelCopyThread) {
        Handler(pixelCopyThread.looper)
    }
    DisposableEffect(Unit) {
        onDispose { pixelCopyThread.quitSafely() }
    }

    LaunchedEffect(view) {
        if (view == null) {
            state.value = null
            return@LaunchedEffect
        }

        val outputView = view.findVideoOutputView() ?: return@LaunchedEffect
        val capturing = AtomicBoolean(false)
        var reusableBitmap: Bitmap? = null

        while (true) {
            // When disabled, skip the capture work (near-zero cost) but keep the last
            // frame so a currently-visible blur does not disappear. Resumes on the next
            // tick once re-enabled.
            if (enabledState.value && !capturing.get()) {
                val w = outputView.width
                val h = outputView.height
                if (w > 0 && h > 0) {
                    val destW = captureWidth
                    val destH = (captureWidth * h.toFloat() / w).toInt().coerceAtLeast(1)

                    when (outputView) {
                        is SurfaceView -> {
                            val dest =
                                reusableBitmap?.takeIf { it.width == destW && it.height == destH }
                                    ?: createBitmap(destW, destH).also {
                                        reusableBitmap = it
                                    }

                            capturing.set(true)
                            try {
                                PixelCopy.request(
                                    outputView,
                                    Rect(0, 0, w, h),
                                    dest,
                                    { result ->
                                        if (result == PixelCopy.SUCCESS) {
                                            state.value = dest.asImageBitmap()
                                        }
                                        capturing.set(false)
                                    },
                                    pixelCopyHandler
                                )
                            } catch (_: Exception) {
                                capturing.set(false)
                            }
                        }

                        is TextureView -> {
                            // PixelCopy can't see a TextureView (no underlying Surface), so
                            // read its current frame directly — cheap at ~48 px wide, and the
                            // blocking readback stays off the UI thread on the handler.
                            capturing.set(true)
                            pixelCopyHandler.post {
                                try {
                                    if (outputView.isAvailable) {
                                        outputView.getBitmap(destW, destH)?.let {
                                            state.value = it.asImageBitmap()
                                        }
                                    }
                                } catch (_: Exception) {
                                } finally {
                                    capturing.set(false)
                                }
                            }
                        }
                    }
                }
            }
            delay(intervalMs)
        }
    }

    return state
}

/**
 * Renders a captured [ImageBitmap] (from [rememberSurfaceCapture]) as a blurred background.
 *
 * The tiny bitmap is stretched to fill and then smoothed by a GPU [RenderEffect] blur shader,
 * producing a real-time frosted-glass effect without overlaying the live SurfaceView.
 *
 * @param bitmap     The captured surface bitmap (typically 48 px wide).
 * @param blurRadius Blur radius in pixels for the [RenderEffect] shader.
 * @param modifier   Modifier applied to the blur container.
 */
@SuppressLint("NewApi")
@Composable
fun SurfaceBlurBackground(
    bitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    blurRadius: Float = 28f
) {
    if (bitmap == null) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                renderEffect = RenderEffect
                    .createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
            .drawBehind {
                drawImage(
                    image = bitmap,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            }
    )
}
