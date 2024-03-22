package com.example.compose_recyclerview.utils

import androidx.recyclerview.widget.RecyclerView

/**
 * Configuration class for customizing ItemTouchHelper behavior in ComposeRecyclerView.
 *
 * @property nonDraggableItemTypes Set of item types that should not be draggable.
 * @property onMove Callback for handling drag-and-drop behavior.
 * @property onSwiped Callback for handling swipe-to-dismiss behavior.
 * @property clearView Callback for handling the cleanup of the view after it is moved or swiped.
 * @property getMovementFlags Callback for providing movement flags for drag and swipe behavior.
 * @property onSelectedChanged Callback for handling changes in the selection state of an item.
 * @property isLongPressDragEnabled Flag to enable or disable long press drag behavior.
 */
class ItemTouchHelperConfig {
    var nonDraggableItemTypes: Set<Int> = emptySet()

    /**
     * Callback for determining whether a ViewHolder can be moved to a new position.
     *
     * @param recyclerView The RecyclerView to which the ItemTouchHelper is attached.
     * @param viewHolder The ViewHolder to be moved.
     * @param target The ViewHolder representing the target position.
     * @return True if the item can be moved, false otherwise.
     */
    var onMove: ((recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) -> Boolean)? = null

    /**
     * Callback for handling swipe-to-dismiss behavior.
     *
     * @param viewHolder The ViewHolder that was swiped.
     * @param direction The direction of the swipe. Can be [ItemTouchHelper.LEFT], [ItemTouchHelper.RIGHT], etc.
     */
    var onSwiped: ((viewHolder: RecyclerView.ViewHolder, direction: Int) -> Unit)? = null

    /**
     * Callback for handling the cleanup of the view after it is moved or swiped.
     *
     * @param recyclerView The RecyclerView to which the ItemTouchHelper is attached.
     * @param viewHolder The ViewHolder to be cleaned up.
     */
    var clearView: ((recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) -> Unit)? = null

    /**
     * Callback for providing movement flags for drag and swipe behavior.
     *
     * @param recyclerView The RecyclerView to which the ItemTouchHelper is attached.
     * @param viewHolder The ViewHolder for which to obtain movement flags.
     * @return A combination of movement flags defined in ItemTouchHelper.
     */
    var getMovementFlags: ((recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) -> Int)? = null

    /**
     * Callback for handling changes in the selection state of an item.
     *
     * @param viewHolder The ViewHolder whose selection state has changed.
     * @param actionState One of [ItemTouchHelper.ACTION_STATE_IDLE], [ItemTouchHelper.ACTION_STATE_SWIPE],
     * or [ItemTouchHelper.ACTION_STATE_DRAG].
     */
    var onSelectedChanged: ((viewHolder: RecyclerView.ViewHolder?, actionState: Int) -> Unit)? = null

    /**
     * Flag to enable or disable long press drag behavior.
     */
    var isLongPressDragEnabled: Boolean = true

    /**
     * Swipe directions for items. If not provided, all directions are enabled by default.
     */
    var swipeDirs: Int? = null

    /**
     * Drag directions for items. If not provided, left or right directions are enabled by default.
     */
    var dragDirs: Int? = null
}
