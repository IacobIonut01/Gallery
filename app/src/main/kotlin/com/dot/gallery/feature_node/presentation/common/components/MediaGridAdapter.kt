package com.dot.gallery.feature_node.presentation.common.components

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dot.gallery.R
import com.dot.gallery.core.presentation.components.StickyHeader
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaItem
import com.dot.gallery.feature_node.presentation.util.FeedbackManager
import com.dot.gallery.feature_node.presentation.util.update
import kotlinx.coroutines.launch

class MediaGridAdapter(
    private val items: List<MediaItem>,
    private val canScroll: Boolean,
    private val allowSelection: Boolean,
    private val selectionState: MutableState<Boolean>,
    private val selectedMedia: SnapshotStateList<Media>,
    private val onItemClick: (Media) -> Unit,
    private val onItemLongClick: (Media) -> Unit,
    private val aboveGridContent: (@Composable () -> Unit)?
) : ListAdapter<MediaItem, RecyclerView.ViewHolder>(MediaItem.Companion.DiffCallback()) {

    class MediaGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(
            media: Media,
            canScroll: Boolean,
            selectionState: MutableState<Boolean>,
            selectedMedia: SnapshotStateList<Media>,
            onItemClick: (Media) -> Unit,
            onItemLongClick: (Media) -> Unit
        ) {
            ComposeView(itemView.context).apply {
                setContent {
                    MediaImage(
                        modifier = Modifier,
                        media = media,
                        selectionState = selectionState,
                        selectedMedia = selectedMedia,
                        canClick = canScroll,
                        onItemClick = onItemClick,
                        onItemLongClick = onItemLongClick
                    )
                }
            }
        }
    }

    class MediaGridHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            date: String,
            data: List<Media>,
            selectionState: MutableState<Boolean>,
            selectedMedia: SnapshotStateList<Media>,
            showAsBig: Boolean,
            allowSelection: Boolean
        ) {
            ComposeView(itemView.context).apply {
                setContent {
                    val feedbackManager = FeedbackManager.rememberFeedbackManager()
                    val scope = rememberCoroutineScope()
                    val isChecked = rememberSaveable { mutableStateOf(false) }
                    if (allowSelection) {
                        LaunchedEffect(selectionState.value) {
                            // Uncheck if selectionState is set to false
                            isChecked.value = isChecked.value && selectionState.value
                        }
                        LaunchedEffect(selectedMedia.size) {
                            // Partial check of media items should not check the header
                            isChecked.value = selectedMedia.containsAll(data)
                        }
                    }

                    val stringToday = stringResource(id = R.string.header_today)
                    val stringYesterday = stringResource(id = R.string.header_yesterday)
                    StickyHeader(
                        date = remember(date) {
                            date
                                .replace("Today", stringToday)
                                .replace("Yesterday", stringYesterday)
                        },
                        showAsBig = showAsBig,
                        isCheckVisible = selectionState,
                        isChecked = isChecked,
                        onChecked = {
                            if (allowSelection) {
                                feedbackManager.vibrate()
                                scope.launch {
                                    isChecked.value = !isChecked.value
                                    if (isChecked.value) {
                                        val toAdd = data.toMutableList().apply {
                                            // Avoid media from being added twice to selection
                                            removeIf { selectedMedia.contains(it) }
                                        }
                                        selectedMedia.addAll(toAdd)
                                    } else selectedMedia.removeAll(data)
                                    selectionState.update(selectedMedia.isNotEmpty())
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    class AboveGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(content: @Composable () -> Unit) {
            ComposeView(itemView.context).apply { setContent(content) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            MEDIA -> MediaGridViewHolder(parent)
            HEADER -> MediaGridHeaderViewHolder(parent)
            ABOVE_GRID_CONTENT -> AboveGridViewHolder(parent)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MediaGridViewHolder -> {
                val item = items[position] as MediaItem.MediaViewItem
                holder.bind(
                    media = item.media,
                    canScroll = canScroll,
                    selectionState = selectionState,
                    selectedMedia = selectedMedia,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick
                )
            }

            is MediaGridHeaderViewHolder -> {
                holder.bind(
                    date = (items[position] as MediaItem.Header).text,
                    selectionState = selectionState,
                    showAsBig = (items[position] as MediaItem.Header).data.size > 3,
                    selectedMedia = selectedMedia,
                    data = (items[position] as MediaItem.Header).data,
                    allowSelection = allowSelection
                )
            }

            is AboveGridViewHolder -> {
                if (aboveGridContent != null) {
                    holder.bind(aboveGridContent)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (aboveGridContent != null && position == 0) return ABOVE_GRID_CONTENT
        return when (items[position]) {
            is MediaItem.MediaViewItem -> MEDIA
            is MediaItem.Header -> HEADER
        }
    }

    override fun getItemCount(): Int = items.size + (aboveGridContent?.let { 1 } ?: 0)

    companion object {
        private const val MEDIA = 0
        private const val HEADER = 1
        private const val ABOVE_GRID_CONTENT = 2
    }
}