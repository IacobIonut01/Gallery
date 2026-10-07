package com.dot.gallery.feature.viewer.components.media

import android.graphics.Bitmap
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.util.MotionPhotoInfo
import com.dot.gallery.feature.viewer.LocalMediaViewerVisualPolicy
import com.github.panpf.zoomimage.compose.zoom.Transform
import kotlin.math.roundToInt
import com.dot.gallery.core.presentation.components.shape
import com.dot.gallery.core.presentation.util.update
import com.dot.gallery.feature.viewer.MotionPhotoController
import com.dot.gallery.feature.viewer.generated.resources.Res
import com.dot.gallery.feature.viewer.generated.resources.frame_picker_shots_heading
import org.jetbrains.compose.resources.stringResource

// ======================== State ========================

/**
 * Holds all motion-photo state: detection, extraction, playback, and thumbnails.
 * Created via [rememberMotionPhotoState] at the screen level so the app bar pill
 * and bottom-sheet filmstrip can share the same state.
 */
@Stable
class MotionPhotoState(
    private val onToggle: () -> Unit = {},
    private val onStart: () -> Unit = {},
    private val onStop: () -> Unit = {},
    private val onSeek: (Long) -> Unit = {},
    private val onSeekAndPause: (Long) -> Unit = {},
) {
    var motionInfo by mutableStateOf<MotionPhotoInfo?>(null)
    var compositeFilmstrip by mutableStateOf<Bitmap?>(null)
    var player by mutableStateOf<ExoPlayer?>(null)
    var isPlaying by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var videoReady by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var videoTransform by mutableStateOf(MotionPhotoVideoTransform.Identity)

    val isDetected: Boolean get() = motionInfo != null

    fun togglePlayback() = onToggle()
    fun startPlayback() = onStart()
    fun stopPlayback() = onStop()
    fun seekTo(ms: Long) = onSeek(ms)
    fun seekAndPause(ms: Long) = onSeekAndPause(ms)
}