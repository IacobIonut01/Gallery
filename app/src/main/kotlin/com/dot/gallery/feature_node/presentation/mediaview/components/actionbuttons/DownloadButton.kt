package com.dot.gallery.feature_node.presentation.mediaview.components.actionbuttons

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dot.gallery.R
import com.dot.gallery.core.LocalMediaHandler
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.launch

@Composable
fun <T : Media> DownloadButton(
    media: T,
    enabled: Boolean,
    followTheme: Boolean = false
) {
    val handler = LocalMediaHandler.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val downloadingText = stringResource(R.string.downloading)
    val downloadCompleteText = stringResource(R.string.download_complete)
    val downloadFailedText = stringResource(R.string.download_failed)
    MediaViewButton(
        currentMedia = media,
        imageVector = Icons.Outlined.Download,
        followTheme = followTheme,
        title = stringResource(R.string.download),
        enabled = enabled
    ) {
        scope.launch {
            Toast.makeText(context, downloadingText, Toast.LENGTH_SHORT).show()
            val result = handler.downloadCloudMedia(listOf(it))
            val count = result.getOrDefault(0)
            if (count > 0) {
                Toast.makeText(context, downloadCompleteText, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, downloadFailedText, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
