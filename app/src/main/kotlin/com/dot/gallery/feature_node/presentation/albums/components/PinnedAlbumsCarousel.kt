/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albums.components

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dot.gallery.R
import com.dot.gallery.feature_node.domain.model.Album
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.MaskableFrameLayout
import kotlin.math.roundToInt

@Composable
fun CarouselPinnedAlbums(
    modifier: Modifier = Modifier,
    albumList: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit
) {
    val primaryTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val containerColor = MaterialTheme.colorScheme.surface.toArgb()
    val configuration = LocalConfiguration.current
    val maxCarouselWidth =
        with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() / 2.75f }
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp),
        factory = {
            RecyclerView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                clipChildren = false
                clipToPadding = false
                adapter = PinnedAlbumsAdapter(
                    onAlbumClick = onAlbumClick,
                    onAlbumLongClick = onAlbumLongClick,
                    maxWidth = maxCarouselWidth,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    containerColor = containerColor
                )
                layoutManager = CarouselLayoutManager()
            }
        },
        update = {
            (it.adapter as PinnedAlbumsAdapter).submitList(albumList)
        }
    )
}

private class PinnedAlbumsAdapter(
    private val onAlbumClick: (Album) -> Unit,
    private val onAlbumLongClick: (Album) -> Unit,
    private val maxWidth: Float,
    private val primaryTextColor: Int,
    private val secondaryTextColor: Int,
    private val containerColor: Int
) :
    ListAdapter<Album, PinnedAlbumsAdapter.ViewHolder>(PinnedAlbumsDiffCallback) {
    inner class ViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        private val albumContainer: View = view.findViewById(R.id.album_container)
        private val albumImage: ImageView = view.findViewById(R.id.carousel_image_view)
        private val albumTitle: TextView = view.findViewById(R.id.album_title)
        private val albumSummary: TextView = view.findViewById(R.id.album_summary)

        fun bind(album: Album) {
            albumContainer.background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(containerColor, Color.TRANSPARENT)
            )
            Glide
                .with(albumImage.context)
                .load(album.pathToThumbnail)
                .into(albumImage)
            albumImage.isClickable = true
            albumImage.setOnClickListener {
                onAlbumClick.invoke(album)
            }
            albumImage.setOnLongClickListener {
                onAlbumLongClick.invoke(album)
                true
            }
            albumTitle.text = album.label
            albumTitle.setTextColor(primaryTextColor)
            albumSummary.text = albumSummary.resources.getQuantityString(
                R.plurals.item_count,
                album.count.toInt(),
                album.count
            )
            albumSummary.setTextColor(secondaryTextColor)
            val maskFrame = itemView as MaskableFrameLayout
            maskFrame.updateLayoutParams {
                width = maxWidth.roundToInt()
            }
            maskFrame.setOnMaskChangedListener { maskRect ->
                // Any custom motion to run when mask size changes
                albumTitle.translationX = maskRect.left
                albumTitle.alpha = lerp(1F, 0F, 0F, 80F, maskRect.left)
                albumSummary.translationX = maskRect.left
                albumSummary.alpha = lerp(1F, 0F, 0F, 80F, maskRect.left)
            }
        }

        private fun lerp(
            outputMin: Float, outputMax: Float, inputMin: Float, inputMax: Float, value: Float
        ): Float {
            if (value <= inputMin) {
                return outputMin
            }
            return if (value >= inputMax) {
                outputMax
            } else lerp(outputMin, outputMax, (value - inputMin) / (inputMax - inputMin))
        }

        private fun lerp(startValue: Float, endValue: Float, fraction: Float): Float {
            return startValue + fraction * (endValue - startValue)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.album_pin_frame, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(getItem(position))
    }

}

private object PinnedAlbumsDiffCallback : DiffUtil.ItemCallback<Album>() {
    override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean {
        return oldItem.id == newItem.id
    }
}