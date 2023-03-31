package com.dot.gallery.feature_node.presentation.standalone.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.shareMedia
import com.dot.gallery.feature_node.presentation.util.toggleFavorite
import com.dot.gallery.ui.theme.Black40P

@Composable
fun BoxScope.StandaloneMediaViewBottomBar(
    showUI: Boolean,
    paddingValues: PaddingValues,
    currentMedia: Media?,
) {
    val context = LocalContext.current
    var favoriteIcon by remember {
        mutableStateOf(
            if (currentMedia != null && currentMedia.favorite == 1)
                Icons.Filled.Favorite
            else Icons.Outlined.FavoriteBorder
        )
    }

    LaunchedEffect(currentMedia) {
        favoriteIcon = if (currentMedia != null && currentMedia.favorite == 1)
            Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    }
    AnimatedVisibility(
        visible = showUI,
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
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Share Component
            BottomBarColumn(
                currentMedia = currentMedia,
                imageVector = Icons.Outlined.Share,
                title = stringResource(R.string.share)
            ) {
                context.shareMedia(media = it)
            }
            // Favorite Component
            BottomBarColumn(
                currentMedia = currentMedia,
                imageVector = favoriteIcon,
                title = stringResource(id = R.string.favorites)
            ) {
                if (context.toggleFavorite(media = it) > 0) {
                    favoriteIcon = Icons.Filled.Favorite
                }
            }
            // Info Component
            BottomBarColumn(
                currentMedia = currentMedia,
                imageVector = Icons.Outlined.Info,
                title = stringResource(R.string.info)
            ) {
                // TODO
            }
        }
    }
}

@Composable
fun BottomBarColumn(
    currentMedia: Media?,
    imageVector: ImageVector,
    title: String,
    onItemClick: (Media) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .height(80.dp)
            .width(90.dp)
            .clickable {
                currentMedia?.let {
                    onItemClick.invoke(it)
                }
            }
            .padding(top = 12.dp, bottom = 16.dp),
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