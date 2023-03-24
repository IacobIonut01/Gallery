package com.dot.gallery.feature_node.presentation.mediaview.components

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.deleteImage
import com.dot.gallery.feature_node.presentation.util.shareImage
import com.dot.gallery.ui.theme.Black40P

@Composable
fun BoxScope.MediaViewBottomBar(
    showUI: MutableState<Boolean>,
    paddingValues: PaddingValues,
    currentMedia: MutableState<Media?>,
    currentIndex: Int,
    deleteResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
    onDeleteMedia: (Int) -> Unit,
) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = showUI.value,
        enter = Constants.Animation.enterAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
        exit = Constants.Animation.exitAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Black40P)
                    )
                )
                .padding(
                    top = 24.dp,
                    bottom = paddingValues.calculateBottomPadding()
                )
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            // Share Component
            BottomBarColumn(
                currentMedia = currentMedia,
                imageVector = Icons.Outlined.Share,
                title = "Share"
            ) {
                context.shareImage(media = it)
            }
            Spacer(modifier = Modifier.size(8.dp))
            // Favourite Component
            BottomBarColumn(
                currentMedia = currentMedia,
                imageVector = Icons.Outlined.FavoriteBorder,
                title = "Favourite"
            ) {
                // TODO
            }
            Spacer(modifier = Modifier.size(8.dp))
            // Trash Component
            BottomBarColumn(
                currentMedia = currentMedia,
                imageVector = Icons.Outlined.DeleteOutline,
                title = "Trash"
            ) {
                context.deleteImage(deleteResultLauncher = deleteResultLauncher, arrayListOf(it))
                onDeleteMedia.invoke(currentIndex)
            }
            Spacer(modifier = Modifier.size(8.dp))
            // Info Component
            BottomBarColumn(
                currentMedia = currentMedia,
                imageVector = Icons.Outlined.Info,
                title = "Info"
            ) {
                // TODO
            }
        }
    }
}

@Composable
private fun BottomBarColumn(
    currentMedia: MutableState<Media?>,
    imageVector: ImageVector,
    title: String,
    onItemClick: (Media) -> Unit
) {
    Column(
        modifier = Modifier
            .height(80.dp)
            .width(90.dp)
            .padding(top = 12.dp, bottom = 16.dp)
            .clickable {
                currentMedia.value?.let {
                    onItemClick.invoke(it)
                }
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = imageVector,
            colorFilter = ColorFilter.tint(Color.White),
            contentDescription = title,
            modifier = Modifier
                .height(32.dp)
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = title,
            modifier = Modifier,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}