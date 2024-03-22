package com.example.compose_recyclerview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.RecyclerView
import com.example.compose_recyclerview.adapter.ComposeRecyclerViewAdapter
import com.example.compose_recyclerview.data.LayoutOrientation
import com.example.compose_recyclerview.utils.InfiniteScrollListener
import com.example.compose_recyclerview.utils.ItemTouchHelperConfig

/**
 * Composable function to display a RecyclerView with dynamically generated Compose items.
 *
 * @param modifier The modifier to be applied to the RecyclerView.
 * @param items The list of items to be displayed in the RecyclerView.
 * @param itemBuilder The lambda function responsible for creating the Compose content for each item at the specified index.
 * @param onScrollEnd Callback triggered when the user reaches the end of the list during scrolling.
 * @param orientation The layout direction of the RecyclerView.
 * @param itemTypeBuilder The optional lambda function to determine the type of each item.
 *  * Required for effective drag and drop. Provide a non-null [ComposeRecyclerViewAdapter.ItemTypeBuilder] when enabling drag and drop functionality.
 *  * Useful when dealing with multiple item types, ensuring proper handling and layout customization for each type.
 * @param onDragCompleted Callback triggered when an item drag operation is completed.
 * @param itemTouchHelperConfig Configuration block for customizing the behavior of ItemTouchHelper.
 *  * Specify non-draggable item types, handle drag-and-drop and swipe actions, customize the appearance during drag, and more.
 * @param onItemMove Callback triggered when an item is moved within the RecyclerView.
 * @param onCreate Callback to customize the RecyclerView after its creation.
 */
@Composable
fun <T> ComposeRecyclerView(
    modifier: Modifier = Modifier,
    items: List<T>,
    itemBuilder: @Composable (item: T, index: Int) -> Unit,
    onScrollEnd: () -> Unit = {},
    layoutManager: RecyclerView.LayoutManager = GridLayoutManager(LocalContext.current, 4),
    orientation: LayoutOrientation = LayoutOrientation.Vertical,
    itemTypeBuilder: ComposeRecyclerViewAdapter.ItemTypeBuilder? = null,
    onDragCompleted: (position: Int) -> Unit = { _ -> },
    itemTouchHelperConfig: (ItemTouchHelperConfig.() -> Unit)? = null,
    onItemMove: (fromPosition: Int, toPosition: Int, itemType: Int) -> Unit = { _, _, _ -> },
    onCreate: (RecyclerView) -> Unit = {}
) {
    val context = LocalContext.current
    var scrollState by rememberSaveable { mutableStateOf(bundleOf()) }

    LaunchedEffect(layoutManager, scrollState) {
        layoutManager.onRestoreInstanceState(scrollState.getParcelable("RecyclerviewState"))
    }

    LaunchedEffect(Unit) {
        if (layoutManager is GridLayoutManager) {
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return itemTypeBuilder?.getItemType(position) ?: 1
                }
            }
        }
    }

    val adapter = remember(layoutManager) {
        ComposeRecyclerViewAdapter<T>().apply {
            this.itemList = items
            this.itemBuilder = itemBuilder
            itemTypeBuilder?.let {
                this.itemTypeBuilder = itemTypeBuilder
            }
            this.layoutOrientation = layoutOrientation
            this.layoutManager = layoutManager
        }
    }

    val composeRecyclerView = remember(layoutManager) {
        RecyclerView(context).apply {
            this.layoutManager = layoutManager
            this.clipToOutline = true
            addOnScrollListener(object : InfiniteScrollListener() {
                override fun onScrollEnd() {
                    onScrollEnd()
                }
            })
            this.adapter = adapter
        }
    }

    val config = remember {
        ItemTouchHelperConfig().apply { itemTouchHelperConfig?.invoke(this) }
    }

    val itemTouchHelper = remember {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            config.dragDirs ?: (UP or DOWN or START or END), config.swipeDirs ?: (LEFT or RIGHT)
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromType = adapter.getItemViewType(viewHolder.bindingAdapterPosition)
                val toType = adapter.getItemViewType(target.bindingAdapterPosition)

                if (fromType != toType || fromType in config.nonDraggableItemTypes) {
                    return false
                }

                config.onMove?.invoke(recyclerView, viewHolder, target)
                    ?: kotlin.run {
                        onItemMove.invoke(
                            viewHolder.bindingAdapterPosition,
                            target.bindingAdapterPosition,
                            fromType
                        )
                        (recyclerView.adapter as ComposeRecyclerViewAdapter<*>).onItemMove(
                            viewHolder.bindingAdapterPosition,
                            target.bindingAdapterPosition
                        )
                    }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                config.onSwiped?.invoke(viewHolder, direction)
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {

                return config.getMovementFlags?.invoke(recyclerView, viewHolder) ?: kotlin.run {
                    val type = adapter.getItemViewType(viewHolder.bindingAdapterPosition)
                    if (type in config.nonDraggableItemTypes) {
                        0
                    } else {
                        super.getMovementFlags(recyclerView, viewHolder)
                    }
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                config.clearView?.invoke(recyclerView, viewHolder) ?: kotlin.run {
                    viewHolder.itemView.alpha = 1f
                    onDragCompleted.invoke(viewHolder.bindingAdapterPosition)
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                config.onSelectedChanged?.invoke(viewHolder, actionState) ?: kotlin.run {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        viewHolder?.itemView?.alpha = 0.5f
                    }
                }
            }

            override fun isLongPressDragEnabled(): Boolean {
                return config.isLongPressDragEnabled
            }
        })
    }

    // Use AndroidView to embed the RecyclerView in the Compose UI
    AndroidView(
        factory = {
            composeRecyclerView.apply {
                onCreate.invoke(this)
                itemTypeBuilder?.let {
                    itemTouchHelper.attachToRecyclerView(this)
                }
            }
        },
        modifier = modifier,
        update = {
            adapter.update(items, itemBuilder, orientation, itemTypeBuilder)
        }
    )

    DisposableEffect(key1 = Unit, effect = {
        onDispose {
            scrollState = bundleOf("RecyclerviewState" to layoutManager.onSaveInstanceState())
        }
    })
}