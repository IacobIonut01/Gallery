package com.example.compose_recyclerview.adapter

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.compose_recyclerview.data.LayoutOrientation
import kotlin.math.max

/**
 * RecyclerView adapter for handling dynamically generated Compose items.
 */
class ComposeRecyclerViewAdapter<T> :
    RecyclerView.Adapter<ComposeRecyclerViewAdapter<T>.ComposeRecyclerViewHolder>() {

    interface ItemTypeBuilder {
        fun getItemType(position: Int): Int
    }

    var itemList: List<T> = mutableListOf()
        set(value) {
            if (field == value) return
            val old = field
            field = value
            notifyItemRangeChange(old)
        }

    var itemBuilder: (@Composable (item: T, index: Int) -> Unit)? =
        null

    var itemTypeBuilder: ItemTypeBuilder? = null

    var layoutOrientation: LayoutOrientation = LayoutOrientation.Vertical
        set(value) {
            if (field == value) return
            field = value
            notifyItemChanged(0)
        }

    var layoutManager: RecyclerView.LayoutManager? = null

    inner class ComposeRecyclerViewHolder(val composeView: ComposeView) :
        RecyclerView.ViewHolder(composeView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeRecyclerViewHolder {
        val context = parent.context
        val composeView = ComposeView(context)
        return ComposeRecyclerViewHolder(composeView)
    }

    override fun onBindViewHolder(holder: ComposeRecyclerViewHolder, position: Int) {
        holder.composeView.apply {
            tag = holder
            setContent {
                if (position < itemList.size) {
                    itemBuilder?.invoke(itemList[position], position)
                }
            }
        }
    }

    override fun getItemCount(): Int = itemList.size

    override fun getItemViewType(position: Int): Int {
        return itemTypeBuilder?.getItemType(position) ?: 0
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        notifyItemMoved(fromPosition, toPosition)
    }

    fun update(
        items: List<T>,
        itemBuilder: @Composable (item: T, index: Int) -> Unit,
        layoutOrientation: LayoutOrientation,
        itemTypeBuilder: ItemTypeBuilder?
    ) {
        this.itemList = items
        this.itemBuilder = itemBuilder
        this.layoutOrientation = layoutOrientation
        itemTypeBuilder?.let {
            this.itemTypeBuilder = it
        }
    }

    private fun notifyItemRangeChange(oldItems: List<T>) {
        val oldSize = oldItems.size
        val newSize = itemList.size
        val firstVisibleIndex = if (layoutManager is LinearLayoutManager?) {
            (layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition() ?: 0
        } else 0
        if (newSize < oldSize) {
            val position = max(0, firstVisibleIndex)
            notifyItemRangeRemoved(position, oldSize - newSize)
        } else if (newSize > oldSize) {
            val start = max(0, firstVisibleIndex)
            notifyItemRangeInserted(start, newSize - oldSize)
        }
    }
}