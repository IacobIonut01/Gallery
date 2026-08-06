package com.dot.gallery.feature_node.presentation.frameextract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.dot.gallery.R
import com.dot.gallery.core.DefaultEventHandler
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.Settings.Misc.getSecureMode
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.launchOpenWithIntent
import com.dot.gallery.feature_node.presentation.util.shareMedia
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FramePickerActivity : ComponentActivity() {
    private val eventHandler = DefaultEventHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = frameSourceExtra()
        if (source == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        enforceSecureFlag(source.sourceKind == FrameSourceKind.VAULT)
        setContent {
            CompositionLocalProvider(LocalEventHandler provides eventHandler) {
                GalleryTheme {
                    LaunchedEffect(Unit) { eventHandler.navigateUpAction = ::finish }
                    val viewModel = hiltViewModel<FramePickerViewModel>()
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(source) { viewModel.initialize(source) }
                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            FramePickerEffect.SelectionLimitReached -> Toast.makeText(
                                this@FramePickerActivity,
                                R.string.frame_picker_selection_limit,
                                Toast.LENGTH_LONG,
                            ).show()
                            is FramePickerEffect.ExportFinished -> {
                                setResult(
                                    Activity.RESULT_OK,
                                    Intent().putStringArrayListExtra(
                                        EXTRA_RESULT_URIS,
                                        ArrayList(effect.savedUris.map { it.toString() }),
                                    ),
                                )
                            }
                        }
                    }
                }
                FramePickerScreen(
                    state = state,
                    onBack = ::finish,
                    onRetry = viewModel::retry,
                    onStep = viewModel::step,
                    onTogglePlayback = viewModel::togglePlayback,
                    onToggleSelection = viewModel::toggleSelection,
                    onJump = viewModel::jumpTo,
                    onRemove = viewModel::removeSelection,
                    onClear = viewModel::clearSelection,
                    onFormat = viewModel::chooseFormat,
                    onExport = viewModel::export,
                    onCancelExport = viewModel::cancelExport,
                    onView = ::viewResult,
                    onShare = ::shareResults,
                )
                }
            }
        }
    }

    private fun viewResult(uri: Uri) {
        lifecycleScope.launch {
            Media.createFromUri(this@FramePickerActivity, uri)?.let {
                launchOpenWithIntent(it)
            }
        }
    }

    private fun shareResults(uris: List<Uri>) {
        lifecycleScope.launch {
            val media = uris.mapNotNull { Media.createFromUri(this@FramePickerActivity, it) }
            if (media.isNotEmpty()) shareMedia(media)
        }
    }

    private fun enforceSecureFlag(forceSecure: Boolean) {
        lifecycleScope.launch {
            getSecureMode(this@FramePickerActivity).collectLatest { enabled ->
                if (forceSecure || enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun frameSourceExtra(): FrameSourceSpec? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SOURCE, FrameSourceSpec::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_SOURCE)
        }

    companion object {
        const val EXTRA_SOURCE = "frame_picker_source"
        const val EXTRA_RESULT_URIS = "frame_picker_result_uris"

        fun launch(context: Context, source: FrameSourceSpec) {
            context.startActivity(
                Intent(context, FramePickerActivity::class.java)
                    .putExtra(EXTRA_SOURCE, source)
                    .apply {
                        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            )
        }
    }
}
