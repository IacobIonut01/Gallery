/**
 * Original from
 * https://stackoverflow.com/a/70853761/12978728
 */
package com.dot.gallery.feature_node.presentation.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun View.vibrate() = reallyPerformHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
fun View.vibrateStrong() = reallyPerformHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

@Suppress("DEPRECATION")
private fun View.reallyPerformHapticFeedback(feedbackConstant: Int) {
    if (context.isTouchExplorationEnabled()) {
        // Don't mess with a blind person's vibrations
        return
    }
    // Either this needs to be set to true, or android:hapticFeedbackEnabled="true" needs to be set in XML
    isHapticFeedbackEnabled = true

    /**
     * [HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING]
     * is deprecated in Android 13+
     */
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        performHapticFeedback(feedbackConstant)
    } else {
        // Most of the constants are off by default: for example, clicking on a button doesn't cause the phone to vibrate anymore
        // if we still want to access this vibration, we'll have to ignore the global settings on that.
        performHapticFeedback(feedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
    }
}

private fun Context.isTouchExplorationEnabled(): Boolean {
    // can be null during unit tests
    val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    return accessibilityManager?.isTouchExplorationEnabled ?: false
}

suspend fun Context.launchEditIntent(media: Media) =
    withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            setDataAndType(media.uri, media.mimeType)
            putExtra("mimeType", media.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.edit)))
    }

suspend fun Context.launchUseAsIntent(media: Media) =
    withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            setDataAndType(media.uri, media.mimeType)
            putExtra("mimeType", media.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.set_as)))
    }


@Composable
fun rememberIsMediaManager(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaStore.canManageMedia(context)
        else false
    }
}

@Composable
fun rememberWindowInsetsController(): WindowInsetsControllerCompat {
    val window = with(LocalContext.current as Activity) { return@with window }
    return remember { WindowCompat.getInsetsController(window, window.decorView) }
}